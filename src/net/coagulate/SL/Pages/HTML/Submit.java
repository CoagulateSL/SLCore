package net.coagulate.SL.Pages.HTML;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author Iain Price
 */
public class Submit extends Container {

	@Nullable
	final String name;
	@Nullable
	String value;

	public Submit(@Nullable final String name) {
		this.name=name;
		value=name;
		super.add(new Raw(name));
	}

	public Submit(@Nullable final String name,
	              @Nullable final String value) {
		this.name=name;
		this.value=value;
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String toHtml(final State st) {
		String v=value;
		if (v==null) { v=""; }
		return "<button type=submit name=\""+name+"\" value=\""+v+"\">"+super.toHtml(st)+"</button>";
	}

	@Override
	public void load(@Nonnull final Map<String,String> map) {
		if (value==null) {
			if (map.containsKey(name)) {
				value=map.get(name);
			}
		}
	}
}
