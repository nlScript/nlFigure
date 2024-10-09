package nlScript.figure;

import ij.ImagePlus;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.io.File;

public interface VectorDocument {

	enum Alignment {
		TOP_LEFT     (VAlignment.TOP,    HAlignment.LEFT),
		CENTER_LEFT  (VAlignment.CENTER, HAlignment.LEFT),
		BOTTOM_LEFT  (VAlignment.BOTTOM, HAlignment.LEFT),
		TOP_CENTER   (VAlignment.TOP,    HAlignment.CENTER),
		CENTERED     (VAlignment.CENTER, HAlignment.CENTER),
		BOTTOM_CENTER(VAlignment.BOTTOM, HAlignment.CENTER),
		TOP_RIGHT    (VAlignment.TOP,    HAlignment.RIGHT),
		CENTER_RIGHT (VAlignment.CENTER, HAlignment.RIGHT),
		BOTTOM_RIGHT (VAlignment.BOTTOM, HAlignment.RIGHT);


		public final VAlignment vAlignment;
		public final HAlignment hAlignment;

		Alignment(VAlignment vAlignment, HAlignment hAlignment) {
			this.vAlignment = vAlignment;
			this.hAlignment = hAlignment;
		}
	}

	enum VAlignment {
		TOP, CENTER, BOTTOM
	}

	enum HAlignment {
		LEFT, CENTER, RIGHT
	}


	void startDocument(File file, double width, double height);

	File endDocument();

	void setFillColor(Color color);

	void setStrokeColor(Color color);

	void setStrokeWidth(double strokeWidth);

	void setFontSize(int fontSize);

	void setFont(Font font);

	void rotateBy(double degrees);

	void makeRectangle(double x, double y, double width, double height,
					   Alignment alignment,
					   boolean draw, boolean fill);

	void makeOval(double x, double y, double width, double height,
				  Alignment alignment,
				  boolean draw, boolean fill);

	void makeImage(ImagePlus image, double x, double y, double width, double height,
				   Alignment alignment,
				   boolean draw, boolean fill);

	Rectangle2D makeText(String text, double x, double y,
				  Alignment alignment);

	Rectangle2D getStringBounds(String text, Font font);

	void newPage();
}
