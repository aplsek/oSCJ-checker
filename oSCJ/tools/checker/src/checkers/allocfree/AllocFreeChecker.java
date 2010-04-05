package checkers.allocfree;

import java.util.Properties;

import com.sun.source.tree.CompilationUnitTree;

import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;

@SuppressWarnings("restriction")
public class AllocFreeChecker extends SourceChecker {

	@Override
	protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree tree) {
		//System.out.println(tree);
		return new AllocFreeVisitor<Void, Void>(this, tree);
	}
	
	@Override
	public Properties getMessages() {
		Properties p = new Properties();
		p.put("unallowed.allocation", "Illegal allocation in a method marked @AllocFree");
		p.put("unallowed.methodcall", "Illegal invocation of non @AllocFree method from within an @AllocFree method");
		p.put("unallowed.foreach", "For each loops not allowed in @AllocFree methods");
		return p;
	}

}
