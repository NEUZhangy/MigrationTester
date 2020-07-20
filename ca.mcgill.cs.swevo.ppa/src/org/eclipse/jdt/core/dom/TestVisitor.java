package org.eclipse.jdt.core.dom;

public class TestVisitor extends ASTVisitor {
	
//	@Override
//	public boolean visit(ArrayAccess node) { //added by nmeng to test the ArrayAccess
//		ITypeBinding type = node.getArray().resolveTypeBinding();
//		if (type == null) {
//			System.out.println("The type is null");
//		} else {
//			System.out.println(type);
//		}
//		return super.visit(node);
//	}

	@Override
	public boolean visit(Assignment node) { //added by nameng, to test the Assignment
//		ITypeBinding type = node.resolveTypeBinding();
//		if (type == null) {
//			System.out.println("left = " + node.getLeftHandSide().resolveTypeBinding());
//			System.out.println("right = " + node.getRightHandSide().resolveTypeBinding());
//		}
//		System.out.println(type.getQualifiedName());
		return super.visit(node);
	}
	
	@Override
	public boolean visit(InfixExpression node) {
//		ITypeBinding type = node.resolveTypeBinding();
//		if (type == null) {
//			System.out.println("unresolved");			
//		} else {
//			System.out.println(type.toString());
//		}
		return super.visit(node);
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		// TODO Auto-generated method stub
		if(node.toString().indexOf("initialize(")>=0){
			IMethodBinding nodeB = node.resolveBinding();
			System.out.println(nodeB.getKey());
			ITypeBinding typeB = nodeB.getDeclaringClass();
			System.out.println(typeB.getQualifiedName());
		}
		return super.visit(node);
	}
	@Override
	public boolean visit(TypeLiteral node) {
		// TODO Auto-generated method stub
//		if(node.toString().indexOf("InitialContextFactory.class")>=0){
//			ITypeBinding nodeB = node.resolveTypeBinding();
//			if(nodeB==null){
//				System.out.println("NULL");
//			}
////			System.out.println("Here");
//		}
		return super.visit(node);
	}
	@Override
	public boolean visit(MethodInvocation node) {
		// TODO Auto-generated method stub
//		  String t = node.toString();
//		    if(t.indexOf("MessageService.getTextMessage")>=0){
//		      IMethodBinding nodeB = node.resolveMethodBinding();
//		      System.out.println(node.resolveTypeBinding());
//		      System.out.println(nodeB);
//		    }
		    return super.visit(node);
	}
	
//	@Override
//	public boolean visit(QualifiedName node) {		
//		if (node.toString().contains("TYPE_FORWARD_ONLY")) {
//			IBinding nodeB = node.resolveBinding();
//		      if(nodeB instanceof IVariableBinding){
//		    	  IVariableBinding varB = (IVariableBinding)nodeB;
//		    	  ITypeBinding type = varB.getDeclaringClass();
//		    	  System.out.println(node.toString());
//		    	  System.out.println(type.toString());
//		    	  System.out.println(node.resolveBinding());
//		      }			
//		}
//		return super.visit(node);
//	}
	
//	@Override
//	  public boolean visit(SimpleName node) {
//	    // TODO Auto-generated method stub
//		try{	   
//		  if (node.toString().equals("args")) {
//			  IBinding nodeB = node.resolveBinding();
//		      if(nodeB instanceof IVariableBinding){
//		    	  IVariableBinding varB = (IVariableBinding)nodeB;
//		    	  System.out.println(node.toString());
//		    	  System.out.println(varB.getDeclaringClass());
//		    	  System.out.println(varB.getType());
//		      }
//		  }
//	     
//		}catch(Exception e) {
////			System.out.print("error occurs");
//		}
//	    return super.visit(node);
//	  }
	
	@Override
	public boolean visit(SuperMethodInvocation node) {
//		IMethodBinding mb = node.resolveMethodBinding();
//		if (mb != null) {
//			System.out.println(mb.getDeclaringClass().getQualifiedName());
//			IMethodBinding[] mbs = mb.getDeclaringClass().getDeclaredMethods();
//			System.out.println(mb.getMethodDeclaration().toString());
//		}
		return super.visit(node);
	}
}
