package nlScript.figure;

import ij.ImagePlus;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.Arrays;

public interface FigureInterface {

	enum Orientation {
		PORTRAIT,
		LANDSCAPE
	}

	enum PageFit {
		FIT_TO_WIDTH,
		FIT_TO_HEIGHT
	}

	enum ImageResize {
		RESIZE_TO_MATCH_SIZE,
		RESIZE_TO_MATCH_MAGNIFICATION
	}

	enum PanelLabelScheme {
		LOWERCASE_LETTERS,
		UPPERCASE_LETTERS,
		NUMBERS
	}

	enum ScalebarPosition {
		LOWER_LEFT,
		LOWER_RIGHT,
		UPPER_LEFT,
		UPPER_RIGHT
	}

	class Size {
		public static Size A0 = new Size(841, 1189);
		public static Size A1 = new Size(594, 841);
		public static Size A2 = new Size(420, 594);
		public static Size A3 = new Size(297, 420);
		public static Size A4 = new Size(210, 297);
		public static Size A5 = new Size(148, 210);

		public final double width;
		public final double height;

		Size(double width, double height) {
			this.width = width;
			this.height = height;
		}

		Size rotate() {
			return new Size(height, width);
		}
	}

	// Document setup
	// --------------
	void setFigureSize(double width, double height);
	void setFigureSize(Size documentSize, Orientation documentOrientation);
	double getFigureWidth();
	double getFigureHeight();
	void setFigureBorder(double top, double left, double bottom, double right);
	double[] getFigureBorder();

	// Title setup
	// -----------
	void setTitle(String title);
	String getTitle();
	void setTitleColor(Color c);
	void setTitleFont(Font font);
	Font getTitleFont();

	// Headings setup
	// --------------
	void setColumnHeader(int column, String header);
	String getColumnHeader(int column);
	void setRowHeader(int row, String header);
	String getRowHeader(int row);
	void setHeaderFont(Font font);
	Font getHeaderFont();
	void setHeaderFrameThickness(double thickness);
	void setHeaderTextColor(Color c);
	void setHeaderFrameColor(Color c);
	void setHeaderBackgroundColor(Color c);
	void setHeaderFrameVisible(boolean b);
	boolean isHeaderFrameVisible();

	// Image setup
	// -----------
	void setImageTitleFont(Font font);
	Font getImageTitleFont();
	void setImageTitleColor(Color c);
	void setImageTitlePosition(VectorDocument.Alignment alignment); // TODO move Alignment declaration here
	void setImageTitleGap(double gap);
	void setImageTitleVisible(boolean v);
	void setImageFrameVisible(boolean b);
	void setImageFrameThickness(double thickness);
	void setImageFrameColor(Color color);
	void setImageScalebarPosition(ScalebarPosition position);
	void setImageScalebarLength(double length);
	void setImageScalebarColor(Color color);
	void setImageScalebarVisible(boolean visible);

	// Panel setup
	// -----------
	void setPanelLabelScheme(PanelLabelScheme scheme);
	void setPanelLabelFont(Font font);
	Font getPanelLabelFont();
	void setPanelLabelColor(Color c);
	void setPanelLabelPosition(VectorDocument.Alignment position);
	void setPanelLabelGap(double gap);
	void setPanelFrameVisible(boolean b);
	void setPanelFrameThickness(double thickness);
	void setPanelFrameColor(Color color);








	void setAllRowsSameHeight(boolean b);
	boolean isAllRowsSameHeight();
	void setAllColumnsSameWidth(boolean b);
	boolean isAllColumnsSameWidth();
	void setFitToPage(PageFit pageFit);
	PageFit getFitToPage();




	void setColumnWidthMM(int column, double mm);
	void setColumnWidthPercent(int column, double percent);

	void setRowHeightMM(int row, double mm);
	void setRowHeightPercent(int row, double percent);

	void addImage(ImagePlus image, int row, int column, ImageResize imageResize);

	void createFigure(File file);

	public static void main(String[] args) {
		Object[] obs = new Object[] {1, 5, 6};

		System.out.println(Arrays.asList(obs).contains(5));
	}
}
