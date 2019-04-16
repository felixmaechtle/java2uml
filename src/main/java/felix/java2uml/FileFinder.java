package felix.java2uml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class FileFinder {
	
	String path;
	Stream<Path> files;
	HashMap<String, String> packages;
	String uml;
	String currentWrapper = "";
	String currentMethods = "";
	String currentVars = "";
	String currentEnumConstants = "";
	
	public FileFinder(String path) {
		this.path = path;
	}
	
	public void findFiles() {
		try {
			files = Files.walk(Paths.get(path))
			.filter(Files::isRegularFile)
			.filter( file -> file.toString().endsWith(".java") && !file.toString().endsWith("module-info.java"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void analyseFiles() {
		if (files == null)
			this.findFiles();
		
		packages = new HashMap<String, String>();
		files.forEach(file -> analyseFile(file));
	}
	
	public void generateUml() {
		if (files == null)
			analyseFiles();
		
		// Finde den Ordner, den alle noch innehaben
		String basePath = "";
		String onePath = "";
		do {
			boolean exitLoop = false;
			
			// Füge inkrementiv einen neuen Ordner hinzu
			for(String path : packages.keySet()) {
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
		} while(packages.keySet().size() != 0);
		
		basePath = basePath.substring(0, basePath.lastIndexOf(File.separator)); // Der letzte Teil soll noch als Verbindung dran sein
		

		uml = "@startuml \n\n";
		// Ordne alles in Packages an:
		for(String path : packages.keySet()) {
			
			String packageName = path.substring(basePath.length() + 1).replace(File.separator, ".");
			packageName = packageName.substring(0, packageName.length() - 1);
			
			uml += "package " + packageName + " {\n"
					+ packages.get(path) + "\n"
					+ "}\n";
		}
		
		uml += "@enduml";
	}
	
	public void analyseFile(Path f) {
		String file = "";
		
		// Read the file
		try {
			file = new String(Files.readAllBytes(f));
		} catch (IOException e) {
			System.out.println("Fehler beim Lesen von " + f.toString());
			e.printStackTrace();
			return;
		}
		
		currentWrapper = "";
		currentMethods = "";
		currentVars = "";
		currentEnumConstants = "";
		
		// Finde alle Elemente der Klasse
		new VoidVisitorAdapter<Object>() {
			
			/**
			 * Eine Klasse oder ein Interface
			 */
            @Override
            public void visit(ClassOrInterfaceDeclaration n, Object arg) {
                super.visit(n, arg);
                
                boolean isAbstract = n.isAbstract();
                boolean isInterface = n.isInterface();
                String name = n.getNameAsString();
                String extendsString = n.getExtendedTypes().size() == 1 ? 
                		" extends " + n.getExtendedTypes().get(0).getNameAsString() : "";
                
                String[] interfaces = new String[n.getImplementedTypes().size()];
                for(int i = 0; i < n.getImplementedTypes().size() - 1; i++) {
                	interfaces[i] = n.getImplementedTypes().get(i).asString();
                }
                
                String implementsString = n.getImplementedTypes().size() > 0 ? 
                		" implements " + String.join(", ", interfaces) : "";
                		
                currentWrapper =
                		(isAbstract ? "abstract " : "") +
                		(isInterface ? "interface " : "class ") +
                		name +
                		extendsString + 
                		implementsString;
            }
            
            
            /**
             * Ein Enum
             */
            @Override 
            public void visit(EnumDeclaration n, Object arg) {
                super.visit(n, arg);
                
                currentWrapper = 
                		"enum " + n.getNameAsString();
            }
            
            /**
             * Eine enum-Konstante
             */
            @Override 
            public void visit(EnumConstantDeclaration n, Object arg) {
                super.visit(n, arg);
                
                currentEnumConstants += "    " + n.getNameAsString() + "\n"; 
            }
           
            
            /**
             * Eine normale Funktion
             */
            @Override
            public void visit(MethodDeclaration n, Object arg) {
                super.visit(n, arg);
                
                String visibility = getAccesibility(n.getAccessSpecifier().toString());
                String returnType = n.getTypeAsString();
                String name = n.getNameAsString();
                
                String[] parameters = new String[n.getParameters().size()];
                for(int i = 0; i < n.getParameters().size() - 1; i++) {
                	parameters[i] = 
                			n.getParameters().get(i).getTypeAsString() + " " + n.getParameters().get(i).getNameAsString();
                }
                String paramStr = Stream.of(parameters)
                        .filter(s -> s != null && !s.isEmpty())
                        .collect(Collectors.joining(", "));
                
                // Baue alles zusammen
                currentMethods += "    " +
                		visibility + 
                		name + 
                		"(" + paramStr + "):" + 
                		returnType + "\n";
            }
           
            /***
             * Ein Atribut
             */
            @Override
            public void visit(FieldDeclaration n, Object arg)
            {   
            	String visibility = getAccesibility(n.getAccessSpecifier().toString());
            	String varName = n.getVariable(0).getNameAsString();
            	String typus = n.getVariable(0).getTypeAsString();
            	
            	currentVars += "    " + visibility + varName + ": " + typus + "\n";
            }
        }.visit(JavaParser.parse(file), null);
        
        if (!currentWrapper.isEmpty()) {
        	String key = new File(f.toString()).getAbsolutePath();
        	key = key.substring(0, key.lastIndexOf(File.separator) + 1);
        	if (!packages.containsKey(key))
        		packages.put(key, "");
        	
        	String packageUml = packages.get(key);
        	packageUml += currentWrapper + " { \n" +
	        		currentVars +
	        		currentEnumConstants + 
	        		currentMethods + 
	        		"}\n\n";
        	
        	//System.out.println(key);
        	packages.put(key, packageUml);
        }
	}

	/**
	 * @return the uml
	 */
	public String getUml() {
		return uml;
	}

	private String getAccesibility(String accessibility) {
		if (accessibility.equals("PRIVATE"))
			return "-";
		if (accessibility.equals("PUBLIC"))
			return "+";
		
		return "FEHLER! : " + accessibility;
	}
}
