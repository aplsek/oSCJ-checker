package checkers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import checkers.scjAllowed.SCJAllowedChecker;
import checkers.scjRestricted.SCJRestrictedChecker;
import checkers.scope.AggregateScopeChecker;
import checkers.source.SourceChecker;
import checkers.util.AggregateChecker;

public class SCJChecker extends AggregateChecker {
	@Override
	protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
		List<Class<? extends SourceChecker>> checkers = new ArrayList<Class<? extends SourceChecker>>();
		checkers.add(SCJRestrictedChecker.class);
		
		//System.out.println(">>>> SCJ ALLOWED checker disabled!!!! <<<<<<<");
	    checkers.add(SCJAllowedChecker.class);
		checkers.add(AggregateScopeChecker.class);
		return checkers;
	}
}
