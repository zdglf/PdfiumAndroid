package com.shockwave.pdfium;

import android.graphics.RectF;

public class PDFAreaModel {
    private int pageIndex;
    private int left;
    private int top;
    private int width;
    private int height;
	public int getPageIndex() {
		return pageIndex;
	}
	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}
	public int getLeft() {
		return left;
	}
	public void setLeft(int left) {
		this.left = left;
	}
	public int getTop() {
		return top;
	}
	public void setTop(int top) {
		this.top = top;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	
	public RectF toRectf(){
		return new RectF(left, top, left+width, top+height);
	}
    
}
