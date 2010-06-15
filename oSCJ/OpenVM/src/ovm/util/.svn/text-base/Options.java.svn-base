package ovm.util;
/**
 * A Properites like object named differently to avoid confusion.
 **/
public class Options extends ovm.core.OVMBase {
    /**
     * A hashmap which keeps track of options specified on the command line
     **/
    protected HashMap options_ = new HashMap();

    /**
     * Return the value of the option <code>opt</code>.
     * @param opt an option String (without leading '-')
     * @return the value of the option, or the empty String ("") if the
     *     option has been set without a value, or null if the option has
     *     not been set.
     **/
    public String getOption(String opt) {
        return (String) options_.get(opt);
    }

    public void setOption(String option, String value) {
	options_.put(option, value);
    }

    public String consumeOption(String opt) {
	String ret = (String) options_.get(opt);
	if (ret != null)
	    options_.remove(opt);
	return ret;
    }


    /** A string representation of the parsed options */
    public String getOptions() {
        StringBuffer buf = new StringBuffer(32*options_.size());
        Set entries = options_.entrySet();
        Iterator iter = entries.iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            buf.append("    ");
            buf.append(entry.getKey());
            buf.append("=");
            buf.append(entry.getValue());
            buf.append("\n");
        }
        return buf.toString();
    }

}

