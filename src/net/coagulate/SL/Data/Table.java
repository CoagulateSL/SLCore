package net.coagulate.SL.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Iain Price
 */
public abstract class Table extends net.coagulate.Core.Database.Table {

	@Nonnull
	@Override
	public final DBConnection getDatabase() { return SL.getDB(); }
}
