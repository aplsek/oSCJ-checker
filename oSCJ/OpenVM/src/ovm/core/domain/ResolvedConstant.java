package ovm.core.domain;

import ovm.core.repository.Constant;

/**
 * A constant-pool entry that has been resolved to an executive domain
 * object.  Not all resovled constants implement the ResolvedConstant
 * interface.  In particular, resolved strings do not implement any
 * special interface, and in the case of user-domain strings are not
 * even objects.
 */
public interface ResolvedConstant extends Constant {
}
