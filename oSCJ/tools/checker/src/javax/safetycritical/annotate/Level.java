package javax.safetycritical.annotate;


public @SCJAllowed enum Level {
	@SCJAllowed
	LEVEL_0 {
		@Override
		public int value() {
			return 0;
		}
	},

	@SCJAllowed
	LEVEL_1 {
		@Override
		public int value() {
			return 1;
		}
	},

	@SCJAllowed
	LEVEL_2 {
		@Override
		public int value() {
			return 2;
		}
	};

	
	public abstract int value();

	
	public static Level getLevel(String value) {
		if ("0".equals(value))
			return LEVEL_0;
		else if ("1".equals(value))
			return LEVEL_1;
		else if ("2".equals(value))
			return LEVEL_2;
		else
			throw new IllegalArgumentException("The value" + value
					+ " is not a legal level.");
	}
}
