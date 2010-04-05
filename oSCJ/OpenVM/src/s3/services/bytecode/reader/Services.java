package s3.services.bytecode.reader;

import ovm.services.bytecode.reader.Parser;

/**
 * Provides an s3 implementation of the bytecode services of Ovm. 
 *  This implementation-specific class is stitched in by the 
 * InivisibleStitcher.
 **/
public final class Services extends ovm.services.bytecode.reader.Services {

    //----fields-----
    final private S3OVMIRParser.Factory oparserF = new S3OVMIRParser.Factory();
    final private S3Parser.Factory parserF = new S3Parser.Factory();

    //----    constructor ------

    public Parser.Factory getOVMIRParserFactory() {
	return oparserF;
    }

    public Parser.Factory getParserFactory() {
	return parserF;
    }

} // End of Services
