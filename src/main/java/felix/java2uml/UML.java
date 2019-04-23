package felix.java2uml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

public class UML {

	private FileFinder f;
	private String uml;

	public UML(String path) {
		this.f = new FileFinder(path);
	}

	public void generateUml() {
		f.analyseFiles();
		
		String basePath = findPackageName();

		uml = "@startuml \n"
				+ "'skinparam classAttributeIconSize 0"
				+ "\n";
		// Ordne alles in Packages an:
		for (String path : f.packages.keySet()) {

			String packageName = path.substring(basePath.length() + 1).replace(File.separator, ".");
			packageName = packageName.substring(0, packageName.length() - 1);

			uml += "package " + packageName 
					+ " {\n" 
					+ f.packages.get(path) 
					+ "\n}\n";
		}
		uml += f.attributeArrows;
		uml += "@enduml";
	}

	public void disableGetterSetter() {
		f.disableGetterSetter = true;
	}
	
	public void enableGetterSetter() {
		f.disableGetterSetter = false;
	}
	
	public String getUml() {
		return uml;
	}

	public String findPackageName() {
		// Finde den Ordner, den alle noch innehaben -> Packagename
		String basePath = "";
		String onePath = "";
		do {
			boolean exitLoop = false;

			// Füge inkrementiv einen neuen Ordner hinzu
			for (String path : f.packages.keySet()) {
				if (!path.startsWith(basePath)) {
					exitLoop = true;
					basePath = basePath.substring(0, basePath.lastIndexOf(File.separator));
				}

				onePath = path;
			}

			if (exitLoop)
				break;

			onePath = onePath.substring(basePath.length());
			onePath = onePath.substring(0, onePath.indexOf(File.separator) + 1);
			basePath += onePath;

		} while (f.packages.keySet().size() != 0);

		return basePath.substring(0, basePath.lastIndexOf(File.separator)); // Der letzte Teil soll noch als Verbindung
																			// dran sein

	}
	
	public String generateSVG() {
    	String svg = "";
    	
    	// Write the first image to "os"
    	try {
    		SourceStringReader reader = new SourceStringReader(uml);
        	final ByteArrayOutputStream os = new ByteArrayOutputStream();
			String desc = reader.generateImage(os, new FileFormatOption(FileFormat.SVG));
			os.close();
			// The XML is stored into svg
	    	svg = new String(os.toByteArray(), Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
    	
    	return svg;
    }

}
