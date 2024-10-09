package nlScript.figure;


import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.WindowOrganizer;
import nlScript.Parser;
import nlScript.core.Autocompletion;
import nlScript.ui.ACEditor;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static nlScript.figure.FigureInterface.ScalebarPosition.*;

/*
Set the title text to 'Mouse intestinal organoids'.
Layout panels to make all rows the same height.
Set the image title invisible.
Modify image 'Organoid 1' to clear the overlay.
Set the panel label color to white.

Set the headings text color to red.
Set the headings text of column 1 to 'SiR-DNA'.
Set the headings text color to green.
Set the headings text of column 2 to 'Alexa-F488'.
Set the headings text color to black.
Set the headings text of column 3 to 'Merge'.

Set the headings text of row 1 to 'Overview'.
Set the headings text of row 2 to 'Zoom'.


Modify image 'Organoid 1' to limit the field of view to (800 x 600) at (0, 0).
Modify image 'Organoid 1' to show a 200 x 150 rectangle at (200, 150).


Modify image 'Organoid 1' to display channels 1.
Add image 'Organoid 1' to panel (1, 1) matching the panel size.

Modify image 'Organoid 1' to display channels 2.
Add image 'Organoid 1' to panel (1, 2) matching the panel size.

Set the image scalebar length to 20.
Set the image scalebar visible.
Modify image 'Organoid 1' to display channels 1, 2.
Add image 'Organoid 1' to panel (1, 3) matching the panel size.
Set the image scalebar invisible.


Modify image 'Organoid 1' to limit the field of view to (200 x 150) at (200, 150).
Modify image 'Organoid 1' to clear the overlay.

Modify image 'Organoid 1' to display channels 1.
Add image 'Organoid 1' to panel (2, 1) matching the panel size.

Modify image 'Organoid 1' to display channels 2.
Add image 'Organoid 1' to panel (2, 2) matching the panel size.

Set the image scalebar visible.
Modify image 'Organoid 1' to display channels 1, 2.
Add image 'Organoid 1' to panel (2, 3) matching the panel size.
 */
public class ScriptedFigure implements PlugIn {

	private FigureInterface figure;

	private enum FontName {
		Helvetica,
		Arial,
		Times,
		Courier
	}

	private enum FontStyle {
		plain(Font.PLAIN),
		bold(Font.BOLD),
		italic(Font.ITALIC);

		private final int style;

		FontStyle(int style) {
			this.style = style;
		}

		int getStyle() {
			return style;
		}
	}

	public void run() {
		final Parser parser = new Parser();

		parser.defineType("title", "'{title:[^']:+}'",
				e -> e.getParsedString("title"),
				true);

		parser.defineType("visibility", "visible", e -> true);
		parser.defineType("visibility", "invisible", e -> false);

		parser.defineType("font-name", FontName.Helvetica.name(), e -> FontName.Helvetica);
		parser.defineType("font-name", FontName.Arial.name(), e -> FontName.Arial);
		parser.defineType("font-name", FontName.Courier.name(), e -> FontName.Courier);

		parser.defineType("font-style", FontStyle.plain.name(), e -> FontStyle.plain);
		parser.defineType("font-style", FontStyle.bold.name(), e -> FontStyle.bold);
		parser.defineType("font-style", FontStyle.italic.name(), e -> FontStyle.italic);

		parser.defineType("font-size", "{font-size:int}pt",
				e -> e.evaluate("font-size"),
				true);

		parser.defineType("font", "{font-size:font-size} {font-style:font-style} {font-name:font-name}", e -> {
			FontName name = (FontName) e.evaluate("font-name");
			int size = (int) e.evaluate("font-size");
			FontStyle style = (FontStyle) e.evaluate("font-style");
			return new Font(name.toString(), style.getStyle(), size);
		}, true);

		parser.defineType("page-size", "A4", e -> FigureInterface.Size.A4);
		parser.defineType("page-size", "A3", e -> FigureInterface.Size.A3);
		parser.defineType("page-size", "A2", e -> FigureInterface.Size.A2);
		parser.defineType("page-size", "A1", e -> FigureInterface.Size.A1);
		parser.defineType("page-size", "A0", e -> FigureInterface.Size.A0);
		parser.defineType("page-size", "A5", e -> FigureInterface.Size.A5);

		parser.defineType("page-orientation", "landscape", e -> FigureInterface.Orientation.LANDSCAPE);
		parser.defineType("page-orientation", "portrait",  e -> FigureInterface.Orientation.PORTRAIT);

		parser.defineType("alignment", VectorDocument.Alignment.TOP_LEFT.name(),       e -> VectorDocument.Alignment.TOP_LEFT);
		parser.defineType("alignment", VectorDocument.Alignment.TOP_CENTER.name(),     e -> VectorDocument.Alignment.TOP_CENTER);
		parser.defineType("alignment", VectorDocument.Alignment.TOP_RIGHT.name(),      e -> VectorDocument.Alignment.TOP_RIGHT);
		parser.defineType("alignment", VectorDocument.Alignment.CENTER_LEFT.name(),    e -> VectorDocument.Alignment.CENTER_LEFT);
		parser.defineType("alignment", VectorDocument.Alignment.CENTERED.name(),       e -> VectorDocument.Alignment.CENTERED);
		parser.defineType("alignment", VectorDocument.Alignment.CENTER_RIGHT.name(),   e -> VectorDocument.Alignment.CENTER_RIGHT);
		parser.defineType("alignment", VectorDocument.Alignment.BOTTOM_LEFT.name(),    e -> VectorDocument.Alignment.BOTTOM_LEFT);
		parser.defineType("alignment", VectorDocument.Alignment.BOTTOM_CENTER.name(),  e -> VectorDocument.Alignment.BOTTOM_CENTER);
		parser.defineType("alignment", VectorDocument.Alignment.BOTTOM_RIGHT.name(),   e -> VectorDocument.Alignment.BOTTOM_RIGHT);

		parser.defineType("label-scheme", FigureInterface.PanelLabelScheme.LOWERCASE_LETTERS.name().toLowerCase(), e -> FigureInterface.PanelLabelScheme.LOWERCASE_LETTERS);
		parser.defineType("label-scheme", FigureInterface.PanelLabelScheme.UPPERCASE_LETTERS.name().toLowerCase(), e -> FigureInterface.PanelLabelScheme.UPPERCASE_LETTERS);
		parser.defineType("label-scheme", FigureInterface.PanelLabelScheme.NUMBERS.name().toLowerCase(),           e -> FigureInterface.PanelLabelScheme.NUMBERS);

		parser.defineType("borders", "{borders:tuple<float,top,left,bottom,right>} mm",
			e -> {
				Object[] borders = (Object[]) e.evaluate("borders");
				return new double[] {
						(double) borders[0],
						(double) borders[1],
						(double) borders[2],
						(double) borders[3]
				};
			},
			true
		);

		// Document setup
		// --------------
		parser.defineSentence("{Set the document} size to {ps:page-size} {po:page-orientation}.", e -> {
			figure.setFigureSize(
					(FigureInterface.Size) e.evaluate("ps"),
					(FigureInterface.Orientation) e.evaluate("po"));
			return null;
		});

		parser.defineSentence("{Set the document} borders to {borders:borders}.", e -> {
			double[] borders = (double[]) e.evaluate("borders");
			figure.setFigureBorder(borders[0], borders[1], borders[2], borders[3]);
			return null;
		});

		// Title setup
		// -----------
		parser.defineSentence("{Set the title} {text} to {title:title}.", e -> {
			figure.setTitle((String) e.evaluate("title"));
			return null;
		});

		parser.defineSentence("{Set the title} {color} to {c:color}.", e -> {
			figure.setTitleColor(new Color((int) e.evaluate("c")));
			return null;
		});

		parser.defineSentence("{Set the title} {font} to {f:font}.", e -> {
			figure.setTitleFont((Font) e.evaluate("f"));
			return null;
		});

		// Headings setup
		// --------------
		parser.defineSentence("{Set the headings} {text} {of column} {c:int} to {h:title}.", e -> {
			figure.setColumnHeader((int) e.evaluate("c") - 1, (String) e.evaluate("h"));
			return null;
		});
		parser.defineSentence("{Set the headings} {text} {of row} {r:int} to {h:title}.", e -> {
			figure.setRowHeader((int) e.evaluate("r") - 1, (String) e.evaluate("h"));
			return null;
		});
		parser.defineSentence("{Set the headings} {text font} to {f:font}.", e -> {
			figure.setHeaderFont((Font) e.evaluate("f"));
			return null;
		});
		parser.defineSentence("{Set the headings} {frame thickness} to {thickness:float}.", e -> {
			figure.setHeaderFrameThickness((double) e.evaluate("thickness"));
			return null;
		});
		parser.defineSentence("{Set the headings} {text color} to {c:color}.", e -> {
			figure.setHeaderTextColor(new Color((int) e.evaluate("c")));
			return null;
		});
		parser.defineSentence("{Set the headings} {frame color} to {c:color}.", e -> {
			figure.setHeaderFrameColor(new Color((int) e.evaluate("c")));
			return null;
		});
		parser.defineSentence("{Set the headings} {background color} to {c:color}.", e -> {
			figure.setHeaderBackgroundColor(new Color((int) e.evaluate("c")));
			return null;
		});
		parser.defineSentence("{Set the headings} {frame invisible}.", e -> {
			figure.setHeaderFrameVisible(false);
			return null;
		});
		parser.defineSentence("{Set the headings} {frame visible}.", e -> {
			figure.setHeaderFrameVisible(true);
			return null;
		});

		// Image setup
		// -----------
		parser.defineSentence("{Set the image title} {font} to {f:font}.", e -> {
			figure.setImageTitleFont((Font) e.evaluate("f"));
			return null;
		});

		parser.defineSentence("{Set the image title} {color} to {c:color}.", e -> {
			figure.setImageTitleColor(new Color((int) e.evaluate("c")));
			return null;
		});

		parser.defineSentence("{Set the image title} {position} to {a:alignment}.", e -> {
			figure.setImageTitlePosition((VectorDocument.Alignment) e.evaluate("a"));
			return null;
		});

		parser.defineSentence("{Set the image title} {gap} to {g:float}.", e -> {
			figure.setImageTitleGap((double) e.evaluate("g"));
			return null;
		});

		parser.defineSentence("{Set the image title} {visible}.", e -> {
			figure.setImageTitleVisible(true);
			return null;
		});

		parser.defineSentence("{Set the image title} {invisible}.", e -> {
			figure.setImageTitleVisible(false);
			return null;
		});

		parser.defineSentence("{Set the image frame} {thickness} to {thickness:float}.", e -> {
			figure.setImageFrameThickness((double) e.evaluate("thickness"));
			return null;
		});

		parser.defineSentence("{Set the image frame} {color} to {c:color}.", e -> {
			figure.setImageFrameColor(new Color((int) e.evaluate("c")));
			return null;
		});

		parser.defineSentence("{Set the image frame} {visible}", e -> {
			figure.setImageFrameVisible(true);
			return null;
		});

		parser.defineSentence("{Set the image frame} {invisible}", e -> {
			figure.setImageFrameVisible(false);
			return null;
		});

		parser.defineType("scalebar-position", "top left",     e -> UPPER_LEFT);
		parser.defineType("scalebar-position", "top right",    e -> UPPER_RIGHT);
		parser.defineType("scalebar-position", "bottom left",  e -> LOWER_LEFT);
		parser.defineType("scalebar-position", "bottom right", e -> LOWER_RIGHT);

		parser.defineSentence("{Set the image scalebar} {position} to {position:scalebar-position}.", e -> {
			figure.setImageScalebarPosition((FigureInterface.ScalebarPosition) e.evaluate("position"));
			return null;
		});

		parser.defineSentence("{Set the image scalebar} {length} to {l:float}.", e -> {
			figure.setImageScalebarLength((double) e.evaluate("l"));
			return null;
		});

		parser.defineSentence("{Set the image scalebar} {color} to {c:color}.", e -> {
			figure.setImageScalebarColor(new Color((int) e.evaluate("color")));
			return null;
		});

		parser.defineSentence("{Set the image scalebar} {visible}.", e -> {
			figure.setImageScalebarVisible(true);
			return null;
		});

		parser.defineSentence("{Set the image scalebar} {invisible}.", e -> {
			figure.setImageScalebarVisible(false);
			return null;
		});

		// Panel setup
		// -----------
		parser.defineSentence("{Set the panel} {label scheme} to {scheme:label-scheme}.", e -> {
			figure.setPanelLabelScheme((FigureInterface.PanelLabelScheme) e.evaluate("scheme"));
			return null;
		});

		parser.defineSentence("{Set the panel} {label font} to {f:font}.", e -> {
			figure.setPanelLabelFont((Font) e.evaluate("f"));
			return null;
		});

		parser.defineSentence("{Set the panel} {label color} to {c:color}.", e -> {
			figure.setPanelLabelColor(new Color((int) e.evaluate("c")));
			return null;
		});

		parser.defineSentence("{Set the panel} {label position} to {a:alignment}.", e -> {
			figure.setPanelLabelPosition((VectorDocument.Alignment) e.evaluate("a"));
			return null;
		});

		parser.defineSentence("{Set the panel} {label gap} to {g:float}.", e -> {
			figure.setPanelLabelGap((double) e.evaluate("g"));
			return null;
		});

		parser.defineSentence("{Set the panel} {frame thickness} to {thickness:float}.", e -> {
			figure.setPanelFrameThickness((double) e.evaluate("thickness"));
			return null;
		});

		parser.defineSentence("{Set the panel} {frame color} to {color:color}.", e -> {
			figure.setPanelFrameColor(new Color((int) e.evaluate("color")));
			return null;
		});

		parser.defineSentence("{Set the panel} {frame visible}.", e -> {
			figure.setPanelFrameVisible(true);
			return null;
		});

		parser.defineSentence("{Set the panel} {frame invisible}.", e -> {
			figure.setPanelFrameVisible(false);
			return null;
		});

		parser.defineType("field-of-view", "({w:int} x {h:int}) at ({x:int}, {y:int})", e -> {
			int x = (int) e.evaluate("x");
			int y = (int) e.evaluate("y");
			int w = (int) e.evaluate("w");
			int h = (int) e.evaluate("h");
			return new Rectangle(x, y, w, h);
		}, true);

		parser.defineSentence("{Layout panels} to make all rows the same height.", e -> {
			figure.setAllRowsSameHeight(true);
			return null;
		});
		parser.defineSentence("{Layout panels} to make all columns the same width.", e -> {
			figure.setAllColumnsSameWidth(true);
			return null;
		});
		parser.defineSentence("{Layout panels} to fit the page height.", e -> {
			figure.setFitToPage(FigureInterface.PageFit.FIT_TO_HEIGHT);
			return null;
		});
		parser.defineSentence("{Layout panels} to fit the page width.", e -> {
			figure.setFitToPage(FigureInterface.PageFit.FIT_TO_WIDTH);
			return null;
		});

		parser.defineType("panel", "{panel:tuple<int,row,column>}", e -> {
			Object[] o = (Object[]) e.evaluate("panel");
			return new int[] { (int) o[0], (int) o[1] };
		});

		parser.defineType("image-magnification", "preserving magnification", e -> FigureInterface.ImageResize.RESIZE_TO_MATCH_MAGNIFICATION);
		parser.defineType("image-magnification", "matching the panel size", e -> FigureInterface.ImageResize.RESIZE_TO_MATCH_SIZE);

		parser.defineType("image", "{image:title}",
				e  -> e.evaluate("image"),
				(e, justCheck) -> Autocompletion.literal(e, Arrays.asList(WindowManager.getImageTitles()), "'", "'"));

		parser.defineSentence("Add image {image:image} to panel {panel:panel} {mag:image-magnification}.", e -> {
			ImagePlus image = WindowManager.getImage((String) e.evaluate("image"));
			int[] panel = (int[]) e.evaluate("panel");
			figure.addImage(
					image,
					panel[0] - 1,
					panel[1] - 1,
					(FigureInterface.ImageResize) e.evaluate("mag"));
			return null;
		});
		// Add image 'title' x to panel b [preserving magnification | resize to fit]

		// Image manipulation
		parser.defineSentence("Modify image {image:image} {to display channels} {channels:list<int>}.", e -> {
			ImagePlus image = WindowManager.getImage((String) e.evaluate("image"));
			List<Object> channels = Arrays.asList((Object[]) e.evaluate("channels"));
			StringBuilder activeChannelsString = new StringBuilder();
			for(int c = 0; c < image.getNChannels(); c++)
				activeChannelsString.append(channels.contains(c + 1) ? '1' : '0');
			System.out.println(activeChannelsString);

			image.setActiveChannels(activeChannelsString.toString());
			image.setDisplayMode(CompositeImage.COMPOSITE);
			return null;
		});

		parser.defineSentence("Modify image {image:image} {to display timepoint} {timepoint:int}.", e -> {
			ImagePlus image = WindowManager.getImage((String) e.evaluate("image"));
			int t = (int) e.evaluate("timepoint");
			image.setT(t);
			return null;
		});

		parser.defineSentence("Modify image {image:image} {to display plane} {plane:int}.", e -> {
			ImagePlus image = WindowManager.getImage((String) e.evaluate("image"));
			int z = (int) e.evaluate("plane");
			image.setZ(z);
			return null;
		});

		parser.defineSentence("Modify image {image:image} {to limit the field of view to} {fov:field-of-view}.", e -> {
			ImagePlus image = WindowManager.getImage((String) e.evaluate("image"));
			Rectangle fov = (Rectangle) e.evaluate("fov");
			image.getCanvas().setSourceRect(fov);
			return null;
		});

		parser.defineType("rectangle", "{w:int} x {h:int} rectangle at ({x:int}, {y:int})", e -> {
			int x = (int) e.evaluate("x");
			int y = (int) e.evaluate("y");
			int w = (int) e.evaluate("w");
			int h = (int) e.evaluate("h");
			return new Rectangle(x, y, w, h);
		}, true);

		parser.defineSentence("Modify image {image:image} to show a {r:rectangle}.", e -> {
			ImagePlus image = WindowManager.getImage((String) e.evaluate("image"));
			Rectangle r = (Rectangle) e.evaluate("r");
			Overlay overlay = image.getOverlay();
			if(overlay == null) {
				overlay = new Overlay();
				image.setOverlay(overlay);
			}
			overlay.add(new Roi(r));
			return null;
		});

		parser.defineSentence("Modify image {image:image} to hide the overlay.", e -> {
			ImagePlus image = WindowManager.getImage((String) e.evaluate("image"));
			image.setHideOverlay(false);
			return null;
		});

		parser.defineSentence("Modify image {image:image} to show the overlay.", e -> {
			ImagePlus image = WindowManager.getImage((String) e.evaluate("image"));
			image.setHideOverlay(true);
			return null;
		});

		parser.defineSentence("Modify image {image:image} to clear the overlay.", e -> {
			ImagePlus image = WindowManager.getImage((String) e.evaluate("image"));
			image.setOverlay(null);
			return null;
		});



		final ACEditor editor = new ACEditor(parser);

		editor.setBeforeRun(() -> {
			figure = new Figure();
		});

		editor.setAfterRun(() -> {
			File tmpFile = null;
			try {
				tmpFile = File.createTempFile("figure", ".pdf");
			} catch (IOException e) {
				e.printStackTrace();
			}
			figure.createFigure(tmpFile);
			try {
				Desktop.getDesktop().open(tmpFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		editor.setVisible(true);
	}

	public void run(String arg) {
		run();
	}

	public static void main(String[] args) {
		/*
			Set the title of the figure to 'The Clowns'.
			Set the document size to A4 landscape.

			Layout panels to fit the page width.

			Set the title of column 1 to 'Column 1'.
			Set the title of column 2 to 'Column 2'.
			Set the title of row 1 to 'Row 1'.
			Set the title of row 2 to 'Row 2'.

			Add image 'Clown 1' to panel (1, 1) matching the panel size.
			Add image 'Clown 1' to panel (2, 1) matching the panel size.
		 */
		new ij.ImageJ();
		ImagePlus clown1 = IJ.openImage("D:\\3Dscript.server_Data\\organoids\\20161114_C7-00.tif");
		clown1.setTitle("Organoid 1");
		clown1.setRoi(50, 50, 50, 50);
		for(ImagePlus ch : ChannelSplitter.split(clown1))
			ch.show();
		// IJ.run(clown1, "Scale Bar...", "width=50 height=1 thickness=2 font=14 color=White background=None location=[Lower Left] horizontal bold hide overlay");
		clown1.show();

		ImagePlus clown2 = IJ.openImage("D:\\3Dscript.server_Data\\organoids\\20161114_C7-01.tif");
		clown2.setTitle("Organoid 2");
		for(ImagePlus ch : ChannelSplitter.split(clown2))
			ch.show();
		clown2.show();

		new WindowOrganizer().run("tile");


		new ScriptedFigure().run();
		// TODO replace roi with overlay
	}
}
