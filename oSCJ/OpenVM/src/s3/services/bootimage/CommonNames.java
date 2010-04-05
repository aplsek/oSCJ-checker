package s3.services.bootimage;
import ovm.core.repository.TypeName;
import ovm.core.repository.RepositoryUtils;

/**
 * Collection of TypeNames useful in the boot process.
 * @author Krzysztof Palacz
 **/
public final class CommonNames {
    public static final TypeName.Scalar Native =
        RepositoryUtils.makeTypeName("ovm/core/execution", "Native");

    public static final TypeName.Scalar VM_Address =
        RepositoryUtils.makeTypeName("ovm/core/services/memory", "VM_Address");

    public static final TypeName.Scalar Oop =
        RepositoryUtils.makeTypeName("ovm/core/domain", "Oop");

    public static final TypeName.Scalar Interpreter =
        RepositoryUtils.makeTypeName("ovm/core/execution", "Interpreter");

    public static final TypeName.Scalar RuntimeExports =
	RepositoryUtils.makeTypeName("ovm/core/execution", "RuntimeExports");

      public static final TypeName.Scalar Launcher = 
	RepositoryUtils.makeTypeName("org/ovmj/java", "Launcher");

    public static final TypeName.Scalar LibraryImports = 
	RepositoryUtils.makeTypeName("ovm/core/execution", "LibraryImports");

}
