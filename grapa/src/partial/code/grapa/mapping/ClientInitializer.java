package partial.code.grapa.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import com.ibm.wala.types.MethodReference;

import partial.code.grapa.tool.JdtUtil;



public class ClientInitializer {
	public String key;
	public String initName;
	public String sig;
	public ASTNode ast;
	
	public Initializer initbody;
	
	public ClientInitializer(String key, String methodName, String sig) {
		// TODO Auto-generated constructor stub
		this.key = key;
		this.initName = methodName;
		this.sig = sig;
	}
	
	//added by shengzhe
	public ClientInitializer(String key, String methodName, Initializer node) {
		// TODO Auto-generated constructor stub
		this.key = key;
		this.initName = methodName;
		initbody = node;
		resolveSig();
	}

	public String getSignature() {
		// TODO Auto-generated method stub
		String line = getTypeName();
		return line+"."+initName+sig;
	}

	public String getTypeName() {
		// TODO Auto-generated method stub
		String line = key.substring(1);
		line = line.replaceAll("/", ".");
		return line;
	}

	public void resolveSig() {
		// TODO Auto-generated method stub
		sig = "()void";
	}
	
	@Override
	public String toString() {
		return initName;
	}
}
