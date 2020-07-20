package edu.vt.cs.grapa.extension;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;

import edu.vt.cs.graph.ClientClass;
import edu.vt.cs.graph.ClientField;
import partial.code.grapa.mapping.ClientInitializer;
import partial.code.grapa.mapping.ClientMethod;
import partial.code.grapa.mapping.ClientMethodVisitor;

public class ClientEntityFinder extends ASTVisitor{

	public Map<String, ClientMethod> methods = new HashMap<String, ClientMethod>();
	public Map<String, ClientField> fields = new HashMap<String, ClientField>();
	public Map<String, ClientClass> classes = new HashMap<String, ClientClass>();
	public Map<String, ClientInitializer> initializer = new HashMap<String, ClientInitializer>();
	public List<String> SuperInterfaceBinding = new LinkedList<String>();
	public String SuperClassBinding = new String();
	private CompilationUnit cu = null;
	private List<MethodDeclaration> virtual_methods = new LinkedList<MethodDeclaration>();
	private int st_pos;
	
	@Override
	public boolean visit(CompilationUnit cu) {
		this.cu = cu;
		return true;
	}
	
	@Override
	public boolean visit(FieldDeclaration node) {
		String typeName = node.getType().toString();
		List<VariableDeclaration> frags = node.fragments();

		for (VariableDeclaration d : frags) {
			IVariableBinding fb = d.resolveBinding();
			if (fb != null) {
				String key = fb.getDeclaringClass().getKey();
				key = key.substring(0, key.length() - 1);
				int mark = key.indexOf("~");
				if (mark > 0) {
					String shortname = key.substring(mark + 1);
					mark = key.lastIndexOf("/");
					String longname = key.substring(0, mark + 1);
					key = longname + shortname;
				}
				key = key.replaceAll("<[a-z;A-z]+>", "");
				key = key.replace(".", "$");
				
				String fieldName = d.getName().getFullyQualifiedName();
				ClientField cf = new ClientField(key, fieldName, d);
				
				StringBuffer buf = new StringBuffer(fb.getDeclaringClass().getQualifiedName()
						+ "." + d.getName().getIdentifier() + " : " + typeName);
				fields.put(buf.toString(), cf);
			}
		}
		return false;
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		// TODO Auto-generated method stub
				IMethodBinding mdb = node.resolveBinding();
				String key = ClientMethodVisitor.getTypeKey(mdb);
				if (key != null) {
					String methodName = ClientMethodVisitor.getName(mdb);
					
					ClientMethod cm = new ClientMethod(key, methodName, node);				
					String qualifiedClassName = key.substring(1);
					qualifiedClassName = qualifiedClassName.replaceAll("/", ".");
					qualifiedClassName = qualifiedClassName.replaceAll("\\$", ".");
					StringBuffer buf = new StringBuffer(qualifiedClassName
							+ "." + node.getName().getIdentifier());
					buf.append("(");
					List<VariableDeclaration> params = node.parameters();
					int i = 0;				
					for (VariableDeclaration p : params) {
						SingleVariableDeclaration d = (SingleVariableDeclaration)p;	
						if (i > 0) {
							buf.append(",");							
						}			
						buf.append(d.getType().toString());
						for (Object extra : d.extraDimensions()) {
							buf.append(extra.toString());
						}
						i++;
					}
					buf.append(")");
					methods.put(buf.toString(), cm);
				}
				return super.visit(node);
	}
	
	public boolean visit(Initializer node) {
//		important code by Prof. Meng
//		System.out.println(node.getStartPosition());
//		ExpressionStatement es = (ExpressionStatement)node.getBody().statements().get(0);
//		Assignment assign = (Assignment)es.getExpression();
//		ClassInstanceCreation ex = (ClassInstanceCreation)assign.getRightHandSide();
//		QualifiedName qName = (QualifiedName)ex.arguments().get(0);
//		IBinding b = qName.resolveBinding();
//		System.out.println(b);
		
		ASTParser parser = ASTParser.newParser(AST.JLS8);
    	parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		Map options = JavaCore.getOptions();
		parser.setCompilerOptions(options);
		
		String str = "class LuceneIndex{ public void Initializer1() "+node.getBody()+"}";
		parser.setSource(str.toCharArray());
		CompilationUnit method = (CompilationUnit) parser.createAST(null);
		
//		System.out.println(method.toString()+" "+method.getNodeType());
		method.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				// TODO Auto-generated method stub
//				System.out.println("MethodDeclaration"+node);
				System.out.println("MethodName: " + node.getName());
				IMethodBinding mbd = node.resolveBinding();
				st_pos = node.getStartPosition() - 8;
				virtual_methods.add(node);
				return super.visit(node);
			}
		});
		MethodDeclaration new_method = virtual_methods.get(0);
//		return visit(new_method);
		
		ClientMethod cm = new ClientMethod("LuceneIndex", "Initializer1", node);
//		ClientInitializer ci = new ClientInitializer("LuceneIndex", "Initializer1", node);
		String na = String.valueOf(node.getStartPosition());
//		initializer.put(na, ci);
		methods.put(na, cm);
		return super.visit(node);
	}

	public boolean visit(TypeDeclaration node) {
		// new ast label
		ITypeBinding tb = node.resolveBinding();
		String key = tb.getKey();
		String className = key.replace('/', '.');
		className = className.replace('$', '.');
		className = className.substring(1);
		className = className.substring(0, className.length() - 1);
		classes.put(className, new ClientClass(key.substring(0, key.length() - 1), className, node));
		if (tb.getSuperclass() != null) {
//			System.out.println(tb.getSuperclass().getBinaryName());
			this.SuperClassBinding = tb.getSuperclass().getBinaryName();
		}
		if (tb.getInterfaces() != null) {
			for (ITypeBinding x: tb.getInterfaces()) {
				this.SuperInterfaceBinding.add(x.getBinaryName());
			}
		}
		return super.visit(node);
	}

}
