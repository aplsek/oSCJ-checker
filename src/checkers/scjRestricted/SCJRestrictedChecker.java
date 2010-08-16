package checkers.scjRestricted;

import java.util.Properties;

import com.sun.source.tree.CompilationUnitTree;

import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;

@SuppressWarnings("restriction")
public class SCJRestrictedChecker extends SourceChecker {

	@Override
	protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree tree) {
		//System.out.println(tree);
		return new SCJRestrictedVisitor<Void, Void>(this, tree);
	}
	
	@Override
	public Properties getMessages() {
		Properties p = new Properties();
		p.put("unallowed.methodcall", "Illegal invocation of a method annotated %s from within a method annotated %s");
		p.put("too.many.values", "@SCJRestricted annotation may only contain one of: %s");
		p.put("illegal.override", "May not override %s with %s");
		
		p.put("unallowed.allocation", "Illegal allocation in a method marked ALLOCATE_FREE");
        p.put("unallowed.foreach", "For each loops not allowed in ALLOCATE_FREE methods");
		return p;
	}
}
