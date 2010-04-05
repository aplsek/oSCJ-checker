package checkers.blockfree;

import java.util.Properties;

import com.sun.source.tree.CompilationUnitTree;

import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;

@SuppressWarnings("restriction")
public class BlockFreeChecker extends SourceChecker {

	@Override
	protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree tree) {
		//System.out.println(tree);
		return new BlockFreeVisitor<Void, Void>(this, tree);
	}
	
	@Override
	public Properties getMessages() {
		Properties p = new Properties();
		p.put("unallowed.methodcall", "Illegal invocation of non @BlockFree method from within an @BlockFree method");
		return p;
	}

}
