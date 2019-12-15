package net.coagulate.SL.Data;

import net.coagulate.Core.Exceptions.SystemException;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class LockTest extends LockableTable {

	public LockTest(final int id) {
		super(id);
		final Integer count = dqi( "select count(*) from locktest where id=?", id);
		if (count == 0) { throw new SystemException("Lock test row " + id + " does not exist"); }
	}

	@Nonnull
	@Override
	public String getTableName() { return "locktest"; }

}
