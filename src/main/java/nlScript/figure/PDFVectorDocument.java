package nlScript.figure;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.RectangleReadOnly;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfFormXObject;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.CustomImageCanvas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PDFVectorDocument implements VectorDocument {

	public static final double PIXEL_PER_MM = PageSize.A4.getWidth() / 210.0;

	private File file;
	private Document document;
	private PdfWriter pdfWriter;
	private Graphics2D graphics;


	private Color strokeColor = Color.BLACK;
	private Color fillColor = Color.WHITE;

	private double strokeWidth = 1.0;

	private Font font = new Font("Helvetica", Font.PLAIN, 10);

	private double angle = 0;

	@Override
	public void startDocument(File file, double width, double height) {
		this.file = file;
		RectangleReadOnly pageSize = new RectangleReadOnly((float)(width * PIXEL_PER_MM), (float)(height * PIXEL_PER_MM));
		document = new Document(pageSize);
		try {
			pdfWriter = PdfWriter.getInstance(document, new FileOutputStream(file));
		} catch (DocumentException | FileNotFoundException e) {
			throw new RuntimeException("Cannot create PDF document " + file, e);
		}
		document.open();
		PdfContentByte cb = pdfWriter.getDirectContent();
		graphics = new PdfGraphics2D(cb, pageSize.getWidth(), pageSize.getHeight());
		graphics.scale(PIXEL_PER_MM, PIXEL_PER_MM);

		setStrokeWidth(strokeWidth);
		setStrokeColor(strokeColor);
		setFillColor(fillColor);
		setFont(font);
	}

	@Override
	public void newPage() {
		graphics.dispose();
		document.newPage();
		PdfContentByte cb = pdfWriter.getDirectContent();
		Rectangle pageSize = pdfWriter.getPageSize();
		graphics = new PdfGraphics2D(cb, pageSize.getWidth(), pageSize.getHeight());
		graphics.scale(PIXEL_PER_MM, PIXEL_PER_MM);

		setStrokeWidth(strokeWidth);
		setStrokeColor(strokeColor);
		setFillColor(fillColor);
		setFont(font);
	}

	@Override
	public File endDocument() {
		graphics.dispose();
		document.close();
		return file;
	}

	@Override
	public void setFillColor(Color color) {
		this.fillColor = color;
	}

	@Override
	public void setStrokeColor(Color color) {
		this.strokeColor = color;
	}

	@Override
	public void setStrokeWidth(double strokeWidth) {
		this.strokeWidth = strokeWidth / PIXEL_PER_MM;
		graphics.setStroke(new BasicStroke((float)this.strokeWidth));
	}

	@Override
	public void setFontSize(int fontSize) {
		this.font = this.font.deriveFont(fontSize);
		graphics.setFont(this.font.deriveFont(fontSize / (float)PIXEL_PER_MM));
		graphics.setFont(font);
	}

	@Override
	public void setFont(Font font) {
		this.font = font;
		graphics.setFont(font.deriveFont(font.getSize() / (float)PIXEL_PER_MM));
	}

	@Override
	public void rotateBy(double degrees) {
		this.angle = degrees;
	}

	private void rotate(double x, double y, double w, double h) {
		if(angle == 0)
			return;

		graphics.rotate(angle * Math.PI / 180, x + w / 2, y + h / 2);
	}

	@Override
	public void makeRectangle(double x, double y, double width, double height,
					   Alignment alignment,
					   boolean draw, boolean fill) {
		Rect2D rect = new Rect2D(x, y, width, height);
		rect.align(alignment, angle);

		AffineTransform tx = graphics.getTransform();
		rotate(rect.x, rect.y, width, height);

		Rectangle2D.Double rectangle = new Rectangle2D.Double(rect.x, rect.y, width, height);
		if(fill) {
			graphics.setColor(fillColor);
			graphics.fill(rectangle);
		}

		if(draw) {
			graphics.setColor(strokeColor);
			graphics.draw(rectangle);
		}
		graphics.setTransform(tx);
	}

	@Override
	public void makeOval(double x, double y, double width, double height,
				  Alignment alignment,
				  boolean draw, boolean fill) {
		Rect2D rect = new Rect2D(x, y, width, height);
		rect.align(alignment, angle);

		AffineTransform tx = graphics.getTransform();
		rotate(rect.x, rect.y, width, height);

		Ellipse2D.Double ellipse = new Ellipse2D.Double(rect.x, rect.y, width, height);
		if(fill) {
			graphics.setColor(fillColor);
			graphics.fill(ellipse);
		}

		if(draw) {
			graphics.setColor(strokeColor);
			graphics.draw(ellipse);
		}
		graphics.setTransform(tx);
	}

	@Override
	public void makeImage(ImagePlus image, double x, double y, double width, double height,
						  Alignment alignment,
						  boolean draw, boolean fill) {
		Rect2D rect = new Rect2D(x, y, width, height);
		rect.align(alignment, angle);

		AffineTransform tx = graphics.getTransform();
		rotate(rect.x, rect.y, width, height);

		AffineTransform t = new AffineTransform();
		t.translate(rect.x, rect.y);
		t.scale(width / image.getWidth(), height / image.getHeight()); // scale = 1

		graphics.transform(t);
		graphics.clipRect(0, 0, image.getWidth(), image.getHeight());

		Graphics2D gCopy = (Graphics2D) graphics.create();

		// graphics.drawImage(image.getImage(), new AffineTransform(), null);
		CustomImageCanvas cic = new CustomImageCanvas(image);
		cic.setMagnification(image.getCanvas().getMagnification());
		cic.setSourceRect(image.getCanvas().getSrcRect());
		cic.setOverlay(image.getOverlay());
		cic.setShowAllList(image.getCanvas().getShowAllList());
		cic.paint(gCopy);
		// if(roi != null) ((Roi)roi.clone()).draw(graphics);
		gCopy.dispose();

		graphics.setClip(null);

		graphics.setTransform(tx);
	}

	@Override
	public Rectangle2D getStringBounds(String text, Font font) {
		FontRenderContext frc = new FontRenderContext(null, true, true);
		font = font.deriveFont(font.getSize() / (float)PIXEL_PER_MM);
		Rectangle2D boundsForHeight = font.getMaxCharBounds(frc);
		Rectangle2D boundsForWidth = font.getStringBounds(text, frc);
		return new Rectangle2D.Double(boundsForWidth.getX(), boundsForHeight.getY(), boundsForWidth.getWidth(), boundsForHeight.getHeight());
	}

	@Override
	public Rectangle2D makeText(String text, double x, double y,
				  Alignment alignment) {
		Rectangle2D bounds = getStringBounds(text, font);
//		System.out.println(bounds);
		double width = bounds.getWidth();
		double height = bounds.getHeight();
		Rect2D rect = new Rect2D(x, y, width, height);
		rect.align(alignment, angle);

		AffineTransform tx = graphics.getTransform();
		rotate(rect.x, rect.y, width, height);

		graphics.setColor(fillColor);
		graphics.drawString(text, (float)(rect.x - bounds.getX()), (float)(rect.y - bounds.getY()));

		graphics.setTransform(tx);

		// makeRectangle(x, y, width, height, alignment, true, false);

		return bounds;
	}

	private static final class Rect2D {
		double x, y, width, height;

		Rect2D(double x, double y, double width, double height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		void align(Alignment alignment, double angle) {
			double rad = angle * Math.PI / 180;
			double wr = width * Math.abs(Math.cos(rad)) + height * Math.abs(Math.sin(rad));
			double hr = width * Math.abs(Math.sin(rad)) + height * Math.abs(Math.cos(rad));
			switch (alignment.hAlignment) {
				case LEFT:   x = x - width / 2 + wr / 2; break;
				case CENTER: x = x - width / 2; break;
				case RIGHT:  x = x - width / 2 - wr / 2; break;
			}
			switch (alignment.vAlignment) {
				case TOP:    y = y - height / 2 + hr / 2; break;
				case CENTER: y = y - height / 2; break;
				case BOTTOM: y = y - height / 2 - hr / 2; break;
			}
		}
	}



	public static void main(String[] args) throws IOException {
		VectorDocument svg = new PDFVectorDocument();
		File file = File.createTempFile("simplepdf", ".pdf");
		svg.startDocument(file, 210, 297);

		svg.setFillColor(Color.LIGHT_GRAY);
		svg.setStrokeColor(Color.GREEN);
		svg.setStrokeWidth(0.5);
		svg.makeRectangle(20, 20, 170, 257, Alignment.TOP_LEFT, true, true);

		svg.setFillColor(Color.black);
		svg.setStrokeColor(Color.black);

		svg.makeRectangle(210 / 2.0, 297 / 2.0, 210, 0.5, Alignment.CENTERED, false, true);
		svg.makeRectangle(210 / 2.0, 297 / 2.0, 0.5, 297, Alignment.CENTERED, false, true);

		svg.makeText("bla", 210 / 2.0,        20, Alignment.TOP_CENTER);
		svg.makeText("bla",  210 - 20, 297 / 2.0, Alignment.CENTER_RIGHT);
		svg.makeText("bla", 210 / 2.0,  297 - 20, Alignment.BOTTOM_CENTER);
		svg.makeText("bla",        20, 297 / 2.0, Alignment.CENTER_LEFT);

		svg.setStrokeColor(Color.PINK);
		svg.setFillColor(Color.PINK.brighter());
		double r = 25;
		for(int i = 0; i < 6; i++) {
			double a = i * Math.PI / 3;
			double x = 210.0 / 2 + r * Math.cos(a);
			double y = 297.0 / 2 + r * Math.sin(a);
//			System.out.println(x + "/" + y);
			svg.makeOval(x, y, 2 * r, 2 * r, Alignment.CENTERED, true, true);
		}
		svg.makeOval(210.0 / 2, 297.0 / 2, 2 * r, 2 * r, Alignment.CENTERED, true, true);


		svg.setStrokeColor(Color.GREEN.darker());
		svg.setFillColor(Color.GREEN.brighter());
		svg.makeRectangle(      20,       20, 10, 10, Alignment.TOP_LEFT,     true, true);
		svg.makeRectangle(210 - 20,       20, 10, 10, Alignment.TOP_RIGHT,    true, true);
		svg.makeRectangle(210 - 20, 297 - 20, 10, 10, Alignment.BOTTOM_RIGHT, true, true);
		svg.makeRectangle(      20, 297 - 20, 10, 10, Alignment.BOTTOM_LEFT,  true, true);

		svg.setStrokeColor(Color.PINK.darker());
		svg.setFillColor(Color.PINK.brighter());
		svg.makeOval(      20,       20, 10, 10, Alignment.TOP_LEFT,     true, true);
		svg.makeOval(210 - 20,       20, 10, 10, Alignment.TOP_RIGHT,    true, true);
		svg.makeOval(210 - 20, 297 - 20, 10, 10, Alignment.BOTTOM_RIGHT, true, true);
		svg.makeOval(      20, 297 - 20, 10, 10, Alignment.BOTTOM_LEFT,  true, true);

		// rotated rectangles and ovals at the center
		for(int i = 0; i < 6; i++) {
			double a = i * 180.0 / 3;
			double x = 210.0 / 2;
			double y = 297.0 / 2;
			double w = 50.0;
			double h = 5.0;
//			System.out.println(x + "/" + y);
			svg.rotateBy(a);
			svg.makeRectangle(x, y, w, h, Alignment.CENTERED, true, false);
			svg.makeOval(     x, y, w, h, Alignment.CENTERED, true, false);
		}

		svg.rotateBy(0); svg.makeRectangle(0, 0, 210, 297, Alignment.TOP_LEFT, true, false);

		// rotated images at the edges
		ImagePlus imp = IJ.openImage("https://imagej.net/images/clown.jpg");
		svg.rotateBy(0);   svg.makeImage(imp, 210 / 2.0,         0, 30, 30, Alignment.TOP_CENTER,    false, false);
		svg.rotateBy(90);  svg.makeImage(imp,       210, 297 / 2.0, 30, 30, Alignment.CENTER_RIGHT,  false, false);
		svg.rotateBy(180); svg.makeImage(imp, 210 / 2.0,       297, 30, 30, Alignment.BOTTOM_CENTER, false, false);
		svg.rotateBy(270); svg.makeImage(imp,         0, 297 / 2.0, 30, 30, Alignment.CENTER_LEFT,   false, false);

		// rotated rectangles at the edges
		svg.rotateBy(0);   svg.makeRectangle(210 / 2.0,         0, 50, 5, Alignment.TOP_CENTER,    true, false);
		svg.rotateBy(90);  svg.makeRectangle(      210, 297 / 2.0, 50, 5, Alignment.CENTER_RIGHT,  true, false);
		svg.rotateBy(180); svg.makeRectangle(210 / 2.0,       297, 50, 5, Alignment.BOTTOM_CENTER, true, false);
		svg.rotateBy(270); svg.makeRectangle(        0, 297 / 2.0, 50, 5, Alignment.CENTER_LEFT,   true, false);

		svg.rotateBy(0);   svg.makeOval(210 / 2.0,         0, 50, 5, Alignment.TOP_CENTER,    true, false);
		svg.rotateBy(90);  svg.makeOval(      210, 297 / 2.0, 50, 5, Alignment.CENTER_RIGHT,  true, false);
		svg.rotateBy(180); svg.makeOval(210 / 2.0,       297, 50, 5, Alignment.BOTTOM_CENTER, true, false);
		svg.rotateBy(270); svg.makeOval(        0, 297 / 2.0, 50, 5, Alignment.CENTER_LEFT,   true, false);

		svg.setStrokeColor(Color.WHITE);
		svg.rotateBy(0);   svg.makeText("XxXxX", 210 / 2.0,         0, Alignment.TOP_CENTER);
		svg.rotateBy(90);  svg.makeText("XxXxX",       210, 297 / 2.0, Alignment.CENTER_RIGHT);
		svg.rotateBy(180); svg.makeText("XxXxX", 210 / 2.0,       297, Alignment.BOTTOM_CENTER);
		svg.rotateBy(270); svg.makeText("XxXxX",         0, 297 / 2.0, Alignment.CENTER_LEFT);

		svg.rotateBy(0);
		svg.setFillColor(Color.black);
		svg.setStrokeColor(Color.black);
		svg.makeText("The End", 210 / 2.0, 297 / 2.0, Alignment.CENTERED);

		svg.makeImage(imp, 20, 20, 50, 50, Alignment.TOP_LEFT, false, false);


		file = svg.endDocument();
		Desktop.getDesktop().open(file);
	}
}
