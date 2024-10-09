package nlScript.figure;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.Duplicator;

import static nlScript.figure.VectorDocument.Alignment;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;


/**
 * Layout is done as follows:
 * - the largest real width of the images within a column defines the column width
 * - the largest real height of the images within a row defines the row height
 * - this defines the dimensions for each cell
 * - the drawing size of an image depends on the alignment parameter
 *   ALIGN_SIZE: the image is resized, preserving aspect ratio, to fit within a cells size
 *   ALIGN_MAGNIFICATION: the image is centered in the middle
 *
 */
public class Figure implements FigureInterface {

	private double borderTop    = 20;
	private double borderBottom = 20;
	private double borderLeft   = 20;
	private double borderRight  = 20;
	private final double gap    = 2;

	private PageFit pageFit = PageFit.FIT_TO_HEIGHT;


	// Title setup
	private String title = null;
	private Font figureTitleFont = new Font("Helvetica", Font.PLAIN, 24);
	private Color figureTitleColor = Color.black;


	// Headings setup
	private Header[] columnHeaders = new Header[0];
	private Header[] rowHeaders = new Header[0];
	private Font headerFont = new Font("Helvetica", Font.PLAIN, 10);
	private double headerFrameThickness = 0.1;
	private Color headerTextColor = Color.black;
	private Color headerFrameColor = Color.BLACK;
	private Color headerBackgroundColor = null;
	private boolean headerFrameVisible = true;


	// Image setup
	private Font imageTitleFont = new Font("Helvetica", Font.PLAIN, 10);
	private Color imageTitleColor = Color.WHITE;
	private Alignment imageTitlePosition = Alignment.BOTTOM_CENTER;
	private double imageTitleGap = 2;
	private boolean imageTitleVisible = true;
	private boolean imageFrameVisible = true;
	private double imageFrameThickness = 0.1;
	private Color imageFrameColor = Color.BLACK;
	private ScalebarPosition scalebarPosition = ScalebarPosition.LOWER_LEFT;
	private double scalebarLength = 0;
	private Color scalebarColor = Color.WHITE;
	private boolean scalebarVisible = false;


	// Panel setup
	private PanelLabelScheme panelLabelScheme = PanelLabelScheme.LOWERCASE_LETTERS;
	private Font panelLabelFont = new Font("Helvetica", Font.PLAIN, 10);
	private Color panelLabelColor = Color.BLACK;
	private Alignment panelLabelPosition = Alignment.TOP_LEFT;
	private double panelLabelGap = 2;
	private boolean panelFrameVisible = false;
	private double panelFrameThickness = 0.1;
	private Color panelFrameColor = Color.BLACK;




	private boolean allRowsSameHeight = false;

	private boolean allColumnsSameWidth = false;

	private Img[][] images = new Img[0][0];

	private Panel[][] panels = new Panel[0][0];



	/** The number of rows of panels */
	private int nRows = 0;

	/** The number of columns of panels */
	private int nColumns = 0;

	/** The width of each column in real world (image) dimensions */
	private double[] columnWidthsRW;

	/** The height of each row in real world (image) dimensions */
	private double[] rowHeightsRW;

	/** The sum of all column widths, in real world (image) dimensions */
	private double totalWidthRW;

	/** The sum of all row heights, in real world (image) dimensions */
	private double totalHeightRW;

	/** From real-world (image) dimensions to pdf pixel dimensions */
	private double scale;

	private VectorDocument backend;


	private double figureWidth = Size.A4.width;

	private double figureHeight = Size.A4.height;


	private void redefineGrid(int nRows, int nColumns) {
		this.nRows = nRows;
		this.nColumns = nColumns;

		Img[][] tmpImages = new Img[nRows][nColumns];
		Panel[][] tmpPanels = new Panel[nRows][nColumns];
		for(int r = 0; r < images.length; r++) {
			System.arraycopy(images[r], 0, tmpImages[r], 0, images[r].length);
			System.arraycopy(panels[r], 0, tmpPanels[r], 0, panels[r].length);
		}
		this.images = tmpImages;
		this.panels = tmpPanels;

		Header[] rowHeadersTmp = new Header[nRows];
		System.arraycopy(rowHeaders, 0, rowHeadersTmp, 0, rowHeaders.length);
		this.rowHeaders = rowHeadersTmp;

		Header[] columnHeadersTmp = new Header[nColumns];
		System.arraycopy(columnHeaders, 0, columnHeadersTmp, 0, columnHeaders.length);
		this.columnHeaders = columnHeadersTmp;
	}

	/*****************
	 * FigureInterface
	 *****************/

	// Document setup
	// --------------
	@Override
	public void setFigureSize(double width, double height) {
		figureWidth = width;
		figureHeight = height;
	}

	@Override
	public void setFigureSize(Size size, Orientation orientation) {
		if(orientation == Orientation.LANDSCAPE)
			size = size.rotate();
		setFigureSize(size.width, size.height);
	}

	@Override
	public double getFigureWidth() {
		return figureWidth;
	}

	@Override
	public double getFigureHeight() {
		return figureHeight;
	}

	@Override
	public void setFigureBorder(double top, double left, double bottom, double right) {
		borderTop = top;
		borderLeft = left;
		borderBottom = bottom;
		borderRight = right;
	}

	@Override
	public double[] getFigureBorder() {
		return new double[] { borderTop, borderLeft, borderBottom, borderRight };
	}

	// Title setup
	// -----------
	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String getTitle() {
		return title;
	}
	@Override
	public void setTitleColor(Color c) {
		figureTitleColor = c;
	}

	@Override
	public void setTitleFont(Font font) {
		figureTitleFont = font;
	}

	@Override
	public Font getTitleFont() {
		return figureTitleFont;
	}

	// Headings setup
	// --------------
	@Override
	public void setColumnHeader(int column, String header) {
		if(column >= nColumns)
			redefineGrid(nRows, column + 1);
		columnHeaders[column] = new Header(
				header, headerFont, headerFrameThickness, headerTextColor, headerFrameColor, headerBackgroundColor, headerFrameVisible);
	}

	@Override
	public String getColumnHeader(int column) {
		return columnHeaders[column].header;
	}

	@Override
	public void setRowHeader(int row, String header) {
		if (row >= nRows)
			redefineGrid(row + 1, nColumns);
		rowHeaders[row] = new Header(
				header, headerFont, headerFrameThickness, headerTextColor, headerFrameColor, headerBackgroundColor, headerFrameVisible);
	}

	@Override
	public String getRowHeader(int row) {
		return rowHeaders[row].header;
	}

	@Override
	public void setHeaderFont(Font font) {
		headerFont = font;
	}

	@Override
	public Font getHeaderFont() {
		return headerFont;
	}

	@Override
	public void setHeaderFrameThickness(double thickness) {
		headerFrameThickness = thickness;
	}

	@Override
	public	void setHeaderTextColor(Color c) {
		headerTextColor = c;
	}

	@Override
	public void setHeaderFrameColor(Color c) {
		headerFrameColor = c;
	}

	@Override
	public void setHeaderBackgroundColor(Color c) {
		headerBackgroundColor = c;
	}

	@Override
	public void setHeaderFrameVisible(boolean b) {
		headerFrameVisible = b;
	}

	@Override
	public boolean isHeaderFrameVisible() {
		return headerFrameVisible;
	}

	// Image setup
	// -----------
	@Override
	public void setImageTitleFont(Font font) {
		imageTitleFont = font;
	}

	@Override
	public Font getImageTitleFont() {
		return imageTitleFont;
	}

	@Override
	public void setImageTitleColor(Color c) {
		imageTitleColor = c;
	}

	@Override
	public void setImageTitlePosition(Alignment alignment) {
		imageTitlePosition = alignment;
	}

	@Override
	public void setImageTitleGap(double gap) {
		imageTitleGap = gap;
	}

	@Override
	public void setImageTitleVisible(boolean v) {
		this.imageTitleVisible = v;
	}

	@Override
	public void setImageFrameVisible(boolean b) {
		this.imageFrameVisible = b;
	}

	@Override
	public void setImageFrameThickness(double thickness) {
		this.imageFrameThickness = thickness;
	}

	@Override
	public void setImageFrameColor(Color color) {
		this.imageFrameColor = color;
	}

	@Override
	public void setImageScalebarPosition(ScalebarPosition position) {
		this.scalebarPosition = position;
	}

	@Override
	public void setImageScalebarLength(double length) {
		this.scalebarLength = length;
	}

	@Override
	public void setImageScalebarColor(Color color) {
		this.scalebarColor = color;
	}

	@Override
	public void setImageScalebarVisible(boolean visible) {
		this.scalebarVisible = visible;
	}

	// Panel setup
	// -----------
	@Override
	public void setPanelLabelScheme(PanelLabelScheme scheme) {
		panelLabelScheme = scheme;
	}

	@Override
	public void setPanelLabelFont(Font font) {
		this.panelLabelFont = font;
	}

	@Override
	public Font getPanelLabelFont() {
		return panelLabelFont;
	}

	@Override
	public void setPanelLabelColor(Color c) {
		panelLabelColor = c;
	}

	@Override
	public void setPanelLabelPosition(Alignment position) {
		panelLabelPosition = position;
	}

	@Override
	public void setPanelLabelGap(double gap) {
		panelLabelGap = gap;
	}

	@Override
	public void setPanelFrameVisible(boolean b) {
		panelFrameVisible = b;
	}

	@Override
	public void setPanelFrameThickness(double thickness) {
		panelFrameThickness = thickness;
	}

	@Override
	public void setPanelFrameColor(Color color) {
		panelFrameColor = color;
	}







	@Override
	public void setAllRowsSameHeight(boolean b) {
		allRowsSameHeight = b;
	}

	@Override
	public boolean isAllRowsSameHeight() {
		return allRowsSameHeight;
	}

	@Override
	public void setAllColumnsSameWidth(boolean b) {
		allColumnsSameWidth = b;
	}

	@Override
	public boolean isAllColumnsSameWidth() {
		return allColumnsSameWidth;
	}

	@Override
	public void setFitToPage(PageFit pageFit) {
		this.pageFit = pageFit;
	}

	@Override
	public PageFit getFitToPage() {
		return pageFit;
	}





	@Override
	public void setColumnWidthMM(int column, double mm) {
		// TODO (don't forget redefineGrid())
	}

	@Override
	public void setColumnWidthPercent(int column, double percent) {
		// TODO (don't forget redefineGrid())
	}

	@Override
	public void setRowHeightMM(int row, double mm) {
		// TODO (don't forget redefineGrid())
	}

	@Override
	public void setRowHeightPercent(int row, double percent) {
		// TODO (don't forget redefineGrid())
	}

	@Override
	public void addImage(ImagePlus image, int row, int column, ImageResize imageResize) {
		if(row >= nRows || column >= nColumns) {
			int nr = Math.max(row + 1, nRows);
			int nc = Math.max(column + 1, nColumns);
			redefineGrid(nr, nc);
		}
		images[row][column] = new Img(image, imageResize,
				imageTitleFont,
				imageTitleColor,
				imageTitlePosition,
				imageTitleGap,
				imageTitleVisible,
				imageFrameVisible,
				imageFrameThickness,
				imageFrameColor,
				scalebarPosition,
				scalebarLength,
				scalebarColor,
				scalebarVisible
		);
		panels[row][column] = new Panel(
				new Rectangle2D.Double(),
				panelLabelFont,
				panelLabelColor,
				panelLabelPosition,
				panelLabelGap,
				panelFrameVisible,
				panelFrameThickness,
				panelFrameColor);
	}

	@Override
	public void createFigure(File file) {
		createPDF(file);
	}

	/************************
	 * End of FigureInterface
	 ************************/

	public void addImage(ImagePlus image, int row, int column) {
		addImage(image, row, column, ImageResize.RESIZE_TO_MATCH_MAGNIFICATION);
	}

	private void calculateColumnWidthsAndHeights() {
		columnWidthsRW = new double[nColumns];
		rowHeightsRW = new double[nRows];
		totalWidthRW = 0;
		totalHeightRW = 0;

		for(int i = 0; i < nColumns; i++) {
			columnWidthsRW[i] = getColumnWidth(i);
			totalWidthRW += columnWidthsRW[i];
		}
		for(int i = 0; i < nRows; i++) {
			rowHeightsRW[i] = getRowHeight(i);
			totalHeightRW += rowHeightsRW[i];
		}
	}

	private double getColumnWidth(int col) {
		double widest = 0;
		int cMin = allColumnsSameWidth ? 0 : col;
		int cMax = allColumnsSameWidth ? nColumns - 1 : col;
		for(int column = cMin; column <= cMax; column++) {
			for(int row = 0; row < nRows; row++) {
				Img img = images[row][col];
				if(img == null)
					continue;
				widest = Math.max(widest, images[row][column].getRealWidth());
			}
		}
		return widest;
	}

	private double getRowHeight(int row) {
		double highest = 0;
		int rMin = allRowsSameHeight ? 0 : row;
		int rMax = allRowsSameHeight ? nRows - 1 : row;
		for(int iRow = rMin; iRow <= rMax; iRow++) {
			for(int col = 0; col < nColumns; col++) {
				if(images[iRow][col] != null)
					highest = Math.max(highest, images[iRow][col].getRealHeight());
			}
		}
		return highest;
	}

	private double calculateHeaderHeight() {
		return backend.getStringBounds("X", headerFont).getHeight();
	}

	private double calculateTitleHeight() {
		return backend.getStringBounds("X", figureTitleFont).getHeight();
	}

	private boolean hasRowHeaders() {
		for (Header rowHeader : rowHeaders) {
			if (rowHeader != null)
				return true;
		}
		return false;
	}

	private boolean hasColumnHeaders() {
		for (Header columnHeader : columnHeaders) {
			if (columnHeader != null)
				return true;
		}
		return false;
	}

	private double calculateFirstPanelTop(int page) {
		double top = borderTop;
		if(hasColumnHeaders())
			top += (calculateHeaderHeight() + gap);
		if(page == 0 && title != null && !title.trim().isEmpty())
			top += calculateTitleHeight() + borderTop / 2;
		return top;
	}

	private double calculateFirstPanelLeft() {
		double left = borderLeft;
		if(hasRowHeaders())
			left += (calculateHeaderHeight() + gap);
		return left;
	}

	/** From real-world (image) dimensions to pdf pixel dimensions */
	private void calculateScale(double w, double h) {
		// calculate available size (without gaps)

		double top = calculateFirstPanelTop(0);
		double left = calculateFirstPanelLeft();

		double availableWidth  = w - left - borderRight  - (nColumns - 1) * gap;
		double availableHeight = h - top  - borderBottom - (nRows    - 1) * gap;

		double scaleX = availableWidth  / totalWidthRW;
		double scaleY = availableHeight / totalHeightRW;
		scale = pageFit == PageFit.FIT_TO_WIDTH ? scaleX : Math.min(scaleX, scaleY);
	}


	private int nPages = 1;
	private int[] rowToPage;

	private void calculatePages(double h) {
		rowToPage = new int[nRows];
		int currentPage = 0;
		double rowTop = calculateFirstPanelTop(currentPage);

		for(int r = 0; r < nRows; r++) {
			double rowHeight = rowHeightsRW[r] * scale;
			double rowBottom = rowTop + rowHeight;
			if(rowBottom > h - borderBottom + 10e-3) {
				currentPage++;
				rowTop = calculateFirstPanelTop(currentPage);
			}
			rowToPage[r] = currentPage;
			rowTop += (rowHeight + gap);
		}
		nPages = currentPage + 1;
	}


	private void calculateTileDimensions() {
		int page = 0;
		double posy = calculateFirstPanelTop(page);
		for(int cy = 0; cy < nRows; cy++) {
			if(rowToPage[cy] != page) {
				page = rowToPage[cy];
				posy = calculateFirstPanelTop(page);
			}
			double posx = calculateFirstPanelLeft();
			for(int cx = 0; cx < nColumns; cx++) {
				Rectangle2D rect = new Rectangle2D.Double(posx, posy, columnWidthsRW[cx] * scale, rowHeightsRW[cy] * scale);
				panels[cy][cx].rect.setRect(rect);
				posx += (columnWidthsRW[cx] * scale + gap);
			}
			posy += (rowHeightsRW[cy] * scale + gap);
		}
	}

	private void drawImages(int page) {
		int labelStart = 'a';
		switch (panelLabelScheme) {
			case NUMBERS: labelStart = '1'; break;
			case LOWERCASE_LETTERS: labelStart = 'a'; break;
			case UPPERCASE_LETTERS: labelStart = 'A'; break;
		}
		labelStart -= 1;

		for(int r = 0; r < nRows; r++) {
			for(int c = 0; c < nColumns; c++) {
				if(rowToPage[r] == page) {
					Panel panel = panels[r][c];
					// TODO draw the panel background


					// draw the image
					Img image = images[r][c];
					if(image != null) {
						image.drawImage(backend, panel.rect);
						labelStart++;
					}

					// draw the panel label
					Rectangle2D.Double tile = (Rectangle2D.Double) panel.rect;
					double labelX = tile.x + tile.width / 2;
					double labelY = tile.y + tile.height;
					switch(panel.panelLabelPosition.hAlignment) {
						case LEFT:   labelX = tile.x + panel.panelLabelGap;              break;
						case RIGHT:  labelX = tile.x + tile.width - panel.panelLabelGap; break;
						case CENTER: labelX = tile.x + tile.width / 2;                   break;
					}
					switch(panelLabelPosition.vAlignment) {
						case TOP:    labelY = tile.y + panel.panelLabelGap;               break;
						case BOTTOM: labelY = tile.y + tile.height - panel.panelLabelGap; break;
						case CENTER: labelY = tile.y + tile.height / 2;                   break;
					}
					backend.setFillColor(panel.panelLabelColor);
					backend.setFont(panel.panelLabelFont);
					backend.makeText(Character.toString((char)labelStart), labelX, labelY, panel.panelLabelPosition);

					// draw the panel frame
					if(panel.panelFrameVisible) {
						backend.setStrokeWidth(panel.panelFrameThickness);
						backend.setStrokeColor(panel.panelFrameColor);
						backend.makeRectangle(tile.x, tile.y, tile.width, tile.height, Alignment.TOP_LEFT, true, false);
					}
				}
			}
		}
	}

	private void adjustDrawingRectangles() {
		for(int r = 0; r < nRows; r++) {
			for(int c = 0; c < nColumns; c++) {
				Img image = images[r][c];
				if(image != null)
					image.adjustDrawingRectangle(columnWidthsRW[c], rowHeightsRW[r], scale);
			}
		}
	}

	private void drawColumnHeaders(int page) {
		double posx = calculateFirstPanelLeft();
		double top  = calculateFirstPanelTop(page);
		for(int i = 0; i < nColumns; i++) {
			Header colh = columnHeaders[i];
			double fw = columnWidthsRW[i] * scale;
			if(colh != null) {
				if (colh.headerFrameVisible) {
					backend.setStrokeWidth(colh.frameThickness);
					backend.setStrokeColor(colh.frameColor);
					boolean headerBg = colh.backgroundColor != null;
					if(headerBg)
						backend.setFillColor(colh.backgroundColor);
					Rectangle2D bounds = backend.getStringBounds(colh.header, colh.font);
					backend.makeRectangle(
							posx + fw / 2,
							top - gap,
							fw,
							bounds.getHeight(),
							Alignment.BOTTOM_CENTER, true, headerBg);
				}
				backend.setFont(colh.font);
				backend.setFillColor(colh.textColor);
				backend.makeText(colh.header, posx + fw / 2, top - gap, Alignment.BOTTOM_CENTER);

			}
			posx += (fw + gap);
		}
	}

	private static class Header {
		private final String header;
		private final Font font;
		private final double frameThickness;
		private final Color textColor;
		private final Color frameColor;
		private final Color backgroundColor;
		private final boolean headerFrameVisible;

		public Header(String header, Font font, double frameThickness, Color textColor, Color frameColor, Color backgroundColor, boolean headerFrameVisible) {
			this.header = header;
			this.font = font;
			this.frameThickness = frameThickness;
			this.textColor = textColor;
			this.frameColor = frameColor;
			this.backgroundColor = backgroundColor;
			this.headerFrameVisible = headerFrameVisible;
		}
	}

	private static class Panel {

		private final Rectangle2D rect;
		private final Font panelLabelFont;
		private final Color panelLabelColor;
		private final Alignment panelLabelPosition;
		private final double panelLabelGap;
		private final boolean panelFrameVisible;
		private final double panelFrameThickness;
		private final Color panelFrameColor;

		public Panel(
				Rectangle2D rect,
				Font panelLabelFont,
				Color panelLabelColor,
				Alignment panelLabelPosition,
				double panelLabelGap,
				boolean panelFrameVisible,
				double panelFrameThickness,
				Color panelFrameColor
		) {
			this.rect = rect;
			this.panelLabelFont = panelLabelFont;
			this.panelLabelColor = panelLabelColor;
			this.panelLabelPosition = panelLabelPosition;
			this.panelLabelGap = panelLabelGap;
			this.panelFrameVisible = panelFrameVisible;
			this.panelFrameThickness = panelFrameThickness;
			this.panelFrameColor = panelFrameColor;
		}
	}

	private void drawRowHeaders(int page) {
		double posx = calculateFirstPanelLeft();
		double posy = calculateFirstPanelTop(page);
		backend.rotateBy(-90);
		for(int i = 0; i < nRows; i++) {
			if(rowToPage[i] != page)
				continue;
			Header rowh = rowHeaders[i];
			double fh = rowHeightsRW[i] * scale;
			if(rowh != null) {
				if (rowh.headerFrameVisible) {
					backend.setStrokeColor(rowh.frameColor);
					backend.setStrokeWidth(rowh.frameThickness);
					boolean headerBg = rowh.backgroundColor != null;
					if(headerBg)
						backend.setFillColor(rowh.backgroundColor);
					Rectangle2D bounds = backend.getStringBounds(rowh.header, rowh.font);
					backend.makeRectangle(posx - gap, posy + fh / 2, fh, bounds.getHeight(), Alignment.CENTER_RIGHT, true, headerBg);
				}
				backend.setFont(rowh.font);
				backend.setFillColor(rowh.textColor);
				backend.makeText(rowh.header, posx - gap, posy + fh / 2, Alignment.CENTER_RIGHT);
			}
			posy += (fh + gap);
		}
		backend.rotateBy(0);
	}

	private void drawTitle(double pageWidth, int page) {
		if(page == 0 && title != null && !title.trim().isEmpty()) {
			backend.setStrokeColor(figureTitleColor);
			backend.setFillColor(figureTitleColor);
			backend.setFont(figureTitleFont);
			backend.makeText(title, pageWidth / 2, borderTop, Alignment.TOP_CENTER);
		}
	}

	public void createPDF(File file) {

		backend = new PDFVectorDocument();
		backend.startDocument(file, figureWidth, figureHeight);

		calculateColumnWidthsAndHeights();
		calculateScale(figureWidth, figureHeight);
		calculatePages(figureHeight);
		calculateTileDimensions();
		adjustDrawingRectangles();

		for(int page = 0; page < nPages; page++) {
			if(page > 0)
				backend.newPage();


			backend.setStrokeWidth(0.5);
			backend.setFillColor(Color.BLACK);

			drawImages(page);
			drawColumnHeaders(page);
			drawRowHeaders(page);

			drawTitle(figureWidth, page);
		}

		backend.endDocument();
	}

	private static class Img {
		final ImagePlus image;
		final double pixelWidth;
		final double pixelHeight;
		final String title;
		final Rectangle2D.Double drawingRectWithinTile = new Rectangle2D.Double();
		final ImageResize imageResize;

		final Font imageTitleFont;
		final Color imageTitleColor;
		final Alignment imageTitlePosition;
		final double imageTitleGap;
		final boolean imageTitleVisible;
		final boolean imageFrameVisible;
		final double imageFrameThickness;
		final Color imageFrameColor;
		final ScalebarPosition scalebarPosition;
		final double scalebarLength;
		final Color scalebarColor;
		final boolean scalebarVisible;

		final Rectangle fov;


		public Img(
				ImagePlus image, ImageResize imageResize,
				Font imageTitleFont,
				Color imageTitleColor,
				Alignment imageTitlePosition,
				double imageTitleGap,
				boolean imageTitleVisible,
				boolean imageFrameVisible,
				double imageFrameThickness,
				Color imageFrameColor,
				ScalebarPosition scalebarPosition,
				double scalebarLength,
				Color scalebarColor,
				boolean scalebarVisible
		) {
			Roi roi = image.getRoi();
			image.killRoi();
			// this.image = image.flatten();
			this.image = new Duplicator().run(image, 1, image.getNChannels(), image.getZ(), image.getZ(), image.getT(), image.getT());
			this.image.show();
			ImageCanvas cic = this.image.getCanvas();
			cic.setMagnification(image.getCanvas().getMagnification());
			cic.setSourceRect(image.getCanvas().getSrcRect());
			// cic.setOverlay(image.getOverlay());
			cic.setShowAllList(image.getCanvas().getShowAllList());
			this.image.getWindow().setVisible(false);

			image.setRoi(roi);
			this.image.setRoi(roi);

			this.pixelWidth = image.getCalibration().pixelWidth;
			this.pixelHeight = image.getCalibration().pixelHeight;
			this.title = image.getTitle();
			this.imageResize = imageResize;

			this.imageTitleFont = imageTitleFont;
			this.imageTitleColor = imageTitleColor;
			this.imageTitlePosition = imageTitlePosition;
			this.imageTitleGap = imageTitleGap;
			this.imageTitleVisible = imageTitleVisible;
			this.imageFrameVisible = imageFrameVisible;
			this.imageFrameThickness = imageFrameThickness;
			this.imageFrameColor = imageFrameColor;

			this.scalebarPosition = scalebarPosition;
			this.scalebarLength = scalebarLength;
			this.scalebarColor = scalebarColor;
			this.scalebarVisible = scalebarVisible;

			fov = image.getCanvas().getSrcRect().getBounds();
		}

		double getRealWidth() {
			return fov.getWidth() * pixelWidth;
		}

		double getRealHeight() {
			return fov.getHeight() * pixelHeight;
		}

		void adjustDrawingRectangle(double colWidth, double rowHeight, double scale) {
			double rw = getRealWidth();
			double rh = getRealHeight();
			switch(imageResize) {
			case RESIZE_TO_MATCH_MAGNIFICATION:
				drawingRectWithinTile.width  = rw;
				drawingRectWithinTile.height = rh;
				drawingRectWithinTile.x      = ( colWidth - rw) / 2.0;
				drawingRectWithinTile.y      = (rowHeight - rh) / 2.0;
				break;
			case RESIZE_TO_MATCH_SIZE:
				double imageAspect = rw / rh;
				double panelAspect = colWidth / rowHeight;
				if(imageAspect > panelAspect) { // some top and bottom padding
					drawingRectWithinTile.width  = colWidth;
					drawingRectWithinTile.height = rh / rw * colWidth;
					drawingRectWithinTile.x      = 0;
					drawingRectWithinTile.y      = (rowHeight - drawingRectWithinTile.height) / 2;
				}
				else { // some left and right padding
					drawingRectWithinTile.height = rowHeight;
					drawingRectWithinTile.width  = rw / rh * rowHeight;
					drawingRectWithinTile.y      = 0;
					drawingRectWithinTile.x      = (colWidth - drawingRectWithinTile.width) / 2;
				}

				break;
			}
			scaleDrawingRectangle(scale);
		}

		void scaleDrawingRectangle(double scale) {
			drawingRectWithinTile.width  *= scale;
			drawingRectWithinTile.height *= scale;
			drawingRectWithinTile.x      *= scale;
			drawingRectWithinTile.y      *= scale;
		}

		void drawImage(VectorDocument g, Rectangle2D panel) {
			Rectangle2D.Double tile = (Rectangle2D.Double) panel;
			double ix = tile.x + drawingRectWithinTile.x;
			double iy = tile.y + drawingRectWithinTile.y;
			double iw = drawingRectWithinTile.width;
			double ih = drawingRectWithinTile.height;
			boolean fill = false;
			if(imageFrameVisible) {
				g.setStrokeColor(imageFrameColor);
				g.setStrokeWidth(imageFrameThickness);
			}
//			image.setActiveChannels(channels);
			image.getCanvas().setSourceRect(fov);
			System.out.println(fov);

			double scale = fov.getWidth() / drawingRectWithinTile.width;

			g.makeImage(image, ix, iy, iw, ih, Alignment.TOP_LEFT, imageFrameVisible, fill);


			// adjust scalebar position to the field of view
			Overlay overlay = this.image.getOverlay();
			if(overlay != null) {
				for (Roi oRoi : overlay) {
					if ("|SB|".equals(oRoi.getName())) {
						String location = oRoi.getProperty("scalebar_loc");
						System.out.println("location = " + location);
						System.out.println("scale = " + scale);
						int x = 0, y = 0;
						switch(location) {
							case "Upper Left":
								x = (int) Math.round(fov.x + 3 * scale);
								y = (int) Math.round(fov.y + 3 * scale);
								break;
							case "Upper Right":
								x = (int) Math.round(fov.x + fov.width - oRoi.getFloatWidth() - 3 * scale);
								y = (int) Math.round(fov.y + 3 * scale);
								break;
							case "Lower Right":
								x = (int) Math.round(fov.x + fov.width - oRoi.getFloatWidth() - 3 * scale);
								y = (int) Math.round(fov.y + fov.height - oRoi.getFloatHeight() - 3 * scale);
								break;
							case "Lower Left":
								x = (int) Math.round(fov.x + 3 / scale);
								y = (int) Math.round(fov.y + fov.height - oRoi.getFloatHeight() - 3 * scale);
								break;
						}
						oRoi.setLocation(x, y);
						g.setFillColor(Color.WHITE);
						double thickness = 0.4;
						double length = 20 / image.getCalibration().pixelWidth;
						g.makeRectangle(ix + 3, iy + tile.height - 3 - thickness, length / scale, thickness, Alignment.TOP_LEFT, false, true);
						System.out.println("new location = " + x + ", " + y);
					}
				}
			}
			if(scalebarVisible && scalebarLength > 0) {
				double thickness = 0.4;
				double offset = 3;
				double x = 0, y = 0;
				double length = scalebarLength / image.getCalibration().pixelWidth / scale;
				switch(scalebarPosition) {
					case LOWER_LEFT:
						x = ix + offset;
						y = iy + tile.height - offset - thickness;
						break;
					case LOWER_RIGHT:
						x = ix + tile.width - offset - length;
						y = iy + tile.height - offset - thickness;
						break;
					case UPPER_LEFT:
						x = ix + offset;
						y = iy + offset;
						break;
					case UPPER_RIGHT:
						x = ix + tile.width - offset - length;
						y = iy + offset;
						break;
				}
				g.setFillColor(scalebarColor);
				g.makeRectangle(x, y, length, thickness, Alignment.TOP_LEFT, false, true);
			}


			if(imageTitleVisible && title != null) {
				ix = tile.x + tile.width / 2;
				iy = tile.y + tile.height;
				switch(imageTitlePosition.hAlignment) {
					case LEFT:   ix = tile.x + imageTitleGap;              break;
					case RIGHT:  ix = tile.x + tile.width - imageTitleGap; break;
					case CENTER: ix = tile.x + tile.width / 2;             break;
				}
				switch(imageTitlePosition.vAlignment) {
					case TOP:    iy = tile.y + imageTitleGap;               break;
					case BOTTOM: iy = tile.y + tile.height - imageTitleGap; break;
					case CENTER: iy = tile.y + tile.height / 2;             break;
				}
				g.setFillColor(imageTitleColor);
				g.setFont(imageTitleFont);
				g.makeText(title, ix, iy, imageTitlePosition);
			}
		}
	}

	@SuppressWarnings("unused")
	private static void test1() throws IOException {
		Figure figure = new Figure();

		ImagePlus a0 = IJ.createImage("A0", "8-bit white", 300, 300, 1);
		ImagePlus a1 = IJ.createImage("A1", 512,  40, 1, 8);
		ImagePlus b0 = IJ.createImage("B0", 512,  40, 1, 8);
		ImagePlus b1 = IJ.createImage("B1",  40, 512, 1, 8);

		figure.addImage(a0, 0, 0);
		figure.addImage(a1, 0, 1);
		figure.addImage(b0, 1, 0);
		figure.addImage(b1, 1, 1);

		figure.setColumnHeader(0, "Column 1");
		figure.setColumnHeader(1, "Column 2");

		figure.setRowHeader(0, "Row 1");
		figure.setRowHeader(1, "Row 2");

		File tmpfile = File.createTempFile("figure", ".pdf");
		figure.setTitle("Title");
		figure.createPDF(tmpfile);
		Desktop.getDesktop().open(tmpfile);
	}

	@SuppressWarnings("unused")
	private static void test2() throws IOException {
		Figure figure = new Figure();

		for(int r = 0; r < 9; r++) {
			figure.setRowHeader(r, "row " + (r + 1));
			for(int c = 0; c < 5; c++) {
				if(r == 0)
					figure.setColumnHeader(c, "col " + (c + 1));
				figure.addImage(IJ.createImage("sdf", 300, 200, 1, 8), r, c);
			}
		}

		File tmpfile = File.createTempFile("figure", ".pdf");
		figure.setTitle("Title");
		figure.createPDF(tmpfile);
		Desktop.getDesktop().open(tmpfile);
	}

	@SuppressWarnings("unused")
	private static void test3() throws IOException {
		Figure figure = new Figure();
		figure.addImage(IJ.createImage("sdf", 300, 300, 1, 8), 0, 0);
		figure.addImage(IJ.createImage("sdf", 300, 300, 1, 8), 0, 1);
		figure.addImage(IJ.createImage("sdf", 300, 300, 1, 8), 0, 2);
		figure.addImage(IJ.createImage("sdf", 100, 100, 1, 8), 1, 0);
		figure.addImage(IJ.createImage("sdf", 100, 100, 1, 8), 1, 1);
		figure.addImage(IJ.createImage("sdf", 100, 100, 1, 8), 1, 2);

		figure.setRowHeader(0, "row 1");
		figure.setRowHeader(1, "row 2");

		figure.setColumnHeader(0, "col 1");
		figure.setColumnHeader(1, "col 2");
		figure.setColumnHeader(2, "col 3");

		File tmpfile = File.createTempFile("figure", ".pdf");
		figure.setTitle("Title");
		figure.createPDF(tmpfile);
		Desktop.getDesktop().open(tmpfile);
	}

	@SuppressWarnings("unused")
	public static void test4() throws IOException {
		Figure figure = new Figure();
		figure.addImage(IJ.createImage("sdf", 300, 300, 1, 8), 0, 0);
//
//		figure.setRowHeader(0, "row 1");
//
//		figure.setColumnHeader(0, "col 1");

//		figure.setTitle("Title");

		File tmpfile = File.createTempFile("figure", ".pdf");
		figure.createPDF(tmpfile);
		Desktop.getDesktop().open(tmpfile);
	}

	public static void test5() throws IOException {
		new ij.ImageJ();
		IJ.openImage("http://imagej.nih.gov/ij/images/clown.jpg").show();

		Figure figure = new Figure();

		figure.setTitle("The Clowns");
		figure.setFigureSize(Size.A4, Orientation.LANDSCAPE);
		figure.setColumnHeader(0, "Column 1");
		figure.setColumnHeader(1, "Column 2");
		figure.setRowHeader(0, "Row 1");
		figure.setRowHeader(1, "Row 2");
		figure.setFitToPage(PageFit.FIT_TO_WIDTH);


		File tmpFile = File.createTempFile("figure", ".pdf");
		figure.setTitle("Title");
		figure.createFigure(tmpFile);
		Desktop.getDesktop().open(tmpFile);
	}

	public static void main(String[] args) throws IOException {
		test4();
	}
}
