package checkers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import checkers.allocfree.AllocFreeChecker;
import checkers.blockfree.BlockFreeChecker;
import checkers.scjAllowed.SCJAllowedChecker;
import checkers.scope.AggregateScopeChecker;
import checkers.source.SourceChecker;
import checkers.util.AggregateChecker;

public class SCJChecker extends AggregateChecker {
	@Override
	protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
		List<Class<? extends SourceChecker>> checkers = new ArrayList<Class<? extends SourceChecker>>();
		checkers.add(AllocFreeChecker.class);
		checkers.add(BlockFreeChecker.class);
		checkers.add(SCJAllowedChecker.class);
		checkers.add(AggregateScopeChecker.class);
		return checkers;
	}
}
