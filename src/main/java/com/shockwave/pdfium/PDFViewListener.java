package com.shockwave.pdfium;

public interface PDFViewListener {
	void onPageChange(PDFView view, int pageIndex);
	void onLongClick(PDFView view, float x, float y);
}
