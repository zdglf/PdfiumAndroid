package com.shockwave.pdfium;

import java.io.File;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


public class PDFView extends View{
	//	private HashMap<Integer, Bitmap> pdfBitmapMap = null;
	private Bitmap pdfBitmap = null;
	private int currentIndex = 0;
	private int totalCount = 0;
	private float scale = 0f;
	//上一个scale
	private float lastScale = 0f;
	private float defaultScale = 0f;
	private float maxScale = 8;
	private int translateX = 0;
	private int translateY = 0;
	private Paint p  =new Paint();
	private int displayWidth = 0;
	private int displayHeight = 0;
	PDFViewListener listener;
	List<PDFAreaModel> tipsModel;
	private float pageWidth = 0;
	private float pageHeight = 0;
	final int REDRAW = 0;
	String filePath = "";
	ExecutorService cachedThreadPool = Executors.newFixedThreadPool(1);
	private int bitmapFactor = 2;

	private float sdkInnerScale = 1.f;
	PdfiumCore core;
	PdfDocument document;
	private Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch(msg.what){
				case REDRAW:
					int page = msg.arg1;
					currentIndex = page;
					if(listener!=null){
						listener.onPageChange(PDFView.this, currentIndex);
					}
					invalidate();
					break;
			}
		};
	};

	public void setTips(List<PDFAreaModel> model){
		this.tipsModel = model;
	}
	public PDFView(Context context, String filePath) {
		super(context);
		this.filePath = filePath;

		initData();
	}
	private void initData() {

//		pdfBitmapMap = new HashMap<Integer, Bitmap>();
		try{
			core = new PdfiumCore(getContext());
			ParcelFileDescriptor fd = ParcelFileDescriptor.open(new File(filePath), ParcelFileDescriptor.MODE_READ_ONLY);
			document = core.newDocument(fd);
			totalCount = core.getPageCount(document);
		}catch(Exception e){
			e.printStackTrace();
			Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
		}


	}

	public float[] getPDFLocation(float x, float y){
		float pdfX = (x - translateX)/(scale*bitmapFactor);
		float pdfY = (y - translateY)/(scale*bitmapFactor);
		pdfY = pageHeight-pdfY;
		return new float[]{pdfX, pdfY};
	}

	private void parsePage(final int page) throws Exception{
		core.openPage(document, page);
		int width = core.getPageWidthPoint(document, page);
		int height = core.getPageHeightPoint(document, page);
		pageWidth = width/sdkInnerScale;
		pageHeight = height/sdkInnerScale;

		if(pdfBitmap!=null){
			pdfBitmap.recycle();
			pdfBitmap = null;
		}
		pdfBitmap = Bitmap.createBitmap((int)(pageWidth*bitmapFactor), (int)(pageHeight*bitmapFactor), Config.ARGB_8888);

		pdfBitmap.eraseColor(Color.WHITE);
		core.renderPageBitmap(document, pdfBitmap, page,0, 0, (int)pageWidth*bitmapFactor, (int)pageHeight*bitmapFactor);
	}

	public void setPage(int page){
		try{
			loadPage(page);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public float getPageWidth(){
		return this.pageWidth;
	}

	public float getPageHeight(){
		return this.pageHeight;
	}
	public int getCurrentPageIndex(){
		return currentIndex;
	}

	public int getPageSize(){
		return totalCount;
	}

	public void setListener(PDFViewListener listener){
		this.listener = listener;
	}

	public void release(){
		if(core!=null){
			try{
				if(document!=null) {
					core.closeDocument(document);
				}
				if(pdfBitmap!=null){
					pdfBitmap.recycle();
					pdfBitmap = null;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}

	}

	private void loadPage(final int page) throws Exception{
//		if(pdfBitmapMap.get(page)!=null){
//			Message msg = new Message();
//			msg.what = REDRAW;
//			msg.arg1 = page;
//			handler.sendMessage(msg);
//			return;
//		}
		cachedThreadPool.execute(new Runnable() {

			@Override
			public void run() {
				try{

					parsePage(page);
					Message msg = new Message();
					msg.what = REDRAW;
					msg.arg1 = page;
					handler.sendMessage(msg);
				}catch(Exception e){
					e.printStackTrace();
				}


			}
		});
	}




	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		displayWidth = canvas.getWidth();
		displayHeight = canvas.getHeight();
//		Bitmap pdfBitmap = pdfBitmapMap.get(currentIndex);
		if(pdfBitmap!=null){
			int pdfWidth = pdfBitmap.getWidth();
			int pdfHeight = pdfBitmap.getHeight();
			//获取最小的缩放比
			if(scale==0f){
				scale = ((float)displayWidth/pdfWidth) < ((float)displayHeight/pdfHeight)?((float)displayWidth/pdfWidth):((float)displayHeight/pdfHeight);
				defaultScale = scale;
			}
			Matrix matrix = new Matrix();
			matrix.postScale(scale, scale);
			matrix.postTranslate(translateX, translateY);
			canvas.drawBitmap(pdfBitmap, matrix, p);
		}
		if(tipsModel!=null){
			for(int i = 0;i < tipsModel.size();i++){
				PDFAreaModel model = tipsModel.get(i);
				float left = model.getLeft();
				float top = pageHeight - model.getTop();
				float width = model.getWidth();
				float height = model.getHeight();
				left *=(scale*bitmapFactor);
				top *=(scale*bitmapFactor);
				width *=(scale*bitmapFactor);
				height *=(scale*bitmapFactor);
				RectF drawRect = new RectF(translateX+left, translateY+top-height, left+width+translateX, top+translateY);
				if(model.getPageIndex() == currentIndex){
					Paint rectP = new Paint();
					rectP.setStyle(Style.STROKE);
					rectP.setStrokeWidth(5);
					rectP.setColor(Color.RED);
					canvas.drawRect(drawRect, rectP);
				}
			}
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		onPDFTouch(event);
		return true;
	}
	boolean isLongClick = false;
	public float []lastx = new float[]{0,0};
	public float []lasty = new float[]{0,0};
	public float []downx = new float[]{0,0};
	public float []downy = new float[]{0,0};
	public float downScale = 0;
	private void onPDFTouch(MotionEvent event) {
		float []tmpx = new float[]{0,0};
		float []tmpy = new float[]{0,0};
		int actionIndex;
		switch (event.getActionMasked()){
			case MotionEvent.ACTION_POINTER_DOWN:
				actionIndex = event.getActionIndex();
				if(actionIndex>=2){
					return;
				}
				lastx[actionIndex] = event.getX(actionIndex);
				lasty[actionIndex] = event.getY(actionIndex);
				downx[actionIndex] = lastx[actionIndex];
				downy[actionIndex] = lasty[actionIndex];
				downScale = scale;

				break;
			case MotionEvent.ACTION_POINTER_UP:
				break;
			case MotionEvent.ACTION_DOWN:
				isLongClick = true;
				lastx[0] = event.getX(0);
				lasty[0] = event.getY(0);
				downx[0] = lastx[0];
				downy[0] = lasty[0];
				break;
			case MotionEvent.ACTION_UP:
				break;
			case MotionEvent.ACTION_MOVE:
				if(event.getPointerCount()==1){
					//单手
					tmpx[0] = event.getX(0);
					tmpy[0] = event.getY(0);
					//移动图片
					float width = bitmapFactor*pageWidth*scale;
					float height = bitmapFactor * pageHeight*scale;
					float testX = translateX;
					float testY = translateY;
					testX -= (lastx[0]-tmpx[0]);
					testY -= (lasty[0]-tmpy[0]);
					if(translateX>0&&testX>0&&testX<translateX){
						translateX = (int)testX;
					}
					if(translateY>0&&testY>0&&testY<translateY){
						translateY = (int)testY;
					}
					if(testX<0&&testX+width>displayWidth){
						translateX = (int)testX;
					}
					if(testY<0&&testY+height>displayHeight){
						translateY = (int)testY;
					}
					if(translateX<0&&testX<0&&testX>translateX){
						translateX = (int)testX;
					}
					if(translateY<0&&testY<0&&testY>translateY){
						translateY = (int)testY;
					}
					invalidate();

					lastx[0] = tmpx[0];
					lasty[0] = tmpy[0];
				}else if(event.getPointerCount()>=2){
					//双手
					tmpx[0] = event.getX(0);
					tmpy[0] = event.getY(0);
					tmpx[1] = event.getX(1);
					tmpy[1] = event.getY(1);

					float width = bitmapFactor*pageWidth*scale;
					float height = bitmapFactor * pageHeight*scale;
					float userScale = distance(tmpx[0], tmpy[0], tmpx[1], tmpy[1])/distance(downx[0], downy[0], downx[1], downy[1]);
					float testScale = userScale*downScale;
					if(testScale<defaultScale){
						testScale= defaultScale;
					}
					if(testScale>maxScale){
						testScale = maxScale;
					}

					float centerX = (downx[0]+downx[1])/2;
					float centerY = (downy[0]+downy[1])/2;
					float testX = (testScale/scale)*(translateX-centerX)+centerX;
					float testY = (testScale/scale)*(translateY-centerY)+centerY;


					if(Math.abs(testScale-scale)>0.01){
						translateX = (int)testX;
						translateY = (int)testY;
						scale = testScale;
					}
					invalidate();
					lastx[0] = tmpx[0];
					lasty[0] = tmpy[0];
					lastx[1] = tmpx[1];
					lasty[1] = tmpy[1];


				}
				break;
		}
	}


	private float distance(float x1, float y1, float x2, float y2){
		return (float) Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
	}
}
