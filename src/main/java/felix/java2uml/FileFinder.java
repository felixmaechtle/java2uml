package felix.java2uml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
	ArrayList<Path> files;
	HashMap<String, String> packages;
	HashMap<String, String> classes;
	String currentWrapper = "";
	String currentMethods = "";
	String currentVars = "";
	String currentEnumConstants = "";
	boolean disableGetterSetter = false;
	String attributeArrows = "";
	String currentClassName;
	ArrayList<String> currentVariables;
	
	public FileFinder(String path) {
		this.path = path;
	}
	
	public void findFiles() {
		try {
			files = (ArrayList<Path>) Files.walk(Paths.get(path))
					.filter(Files::isRegularFile)
					.filter( file -> file.toString().endsWith(".java") && !file.toString().endsWith("module-info.java"))
					.collect(Collectors.toList());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void analyseFiles() {
		if (files == null)
			this.findFiles();
		
		packages = new HashMap<String, String>();
		classes = new HashMap<String, String>();

		files.forEach(f -> classes.put(f.getFileName().toString().replace(".java", ""), "1"));
		files.forEach(f -> analyseFile(f));
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
		currentClassName = "";
		currentVariables = new ArrayList<String>();
		
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
                currentClassName = name;
                String extendsString = 
                		n.getExtendedTypes().size() == 1 
                		&& classes.containsKey(getRealClassName(n.getExtendedTypes().get(0).getNameAsString())) ? 
                		" extends " + getRealClassName(n.getExtendedTypes().get(0).getNameAsString()) : "";
                
                String[] interfaces = new String[n.getImplementedTypes().size()];
                for(int i = 0; i < n.getImplementedTypes().size(); i++) {
                	String typus = getRealClassName(n.getImplementedTypes().get(i).asString());
                	if (classes.containsKey(typus)) {
                    	interfaces[i] = typus;
                	}
                }
                
                String implementsString = n.getImplementedTypes().size() > 0 ? 
                		" implements " + String.join(", ", interfaces) : "";
                		
                currentWrapper =
                		(isAbstract ? "abstract " : "") +
                		(isInterface ? "interface " : "class ") +
                		"\"" + name + "\"" +
                		" as " + name +
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
                
                if ((name.startsWith("get") || name.startsWith("set")) && disableGetterSetter)
                	return;
                
                String[] parameters = new String[n.getParameters().size()];
                for(int i = 0; i <= n.getParameters().size() - 1; i++) {
               		parameters[i] = 
               			n.getParameters().get(i).getTypeAsString() 
               			+ " " 
             			+ n.getParameters().get(i).getNameAsString();
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
            	
            	if (classes.containsKey(typus)) {
            		currentVariables.add( typus);
            	}
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
        	
        	// Füge die Pfeile ein
        	for(String vars : currentVariables) {
        		attributeArrows += currentClassName + " - " + vars + "\n";
        	}
        	
        	packages.put(key, packageUml);
        }
	}
	
	private String getAccesibility(String accessibility) {
		if (accessibility.equals("PRIVATE"))
			return "-";
		if (accessibility.equals("PUBLIC"))
			return "+";
		if (accessibility.equals("PROTECTED"))
			return "#";
		
		return "";
	}
	
	private String getRealClassName(String extendingClass) {
		if (extendingClass.indexOf("<") != -1) {
			extendingClass = extendingClass.substring(extendingClass.indexOf("<") + 1);
			extendingClass = extendingClass.substring(0, extendingClass.indexOf(">"));
		}
		
		return extendingClass;
	}
	
}
