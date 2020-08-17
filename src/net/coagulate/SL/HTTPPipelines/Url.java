package net.coagulate.SL.HTTPPipelines;

import javax.annotation.Nonnull;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface Url {
    @Nonnull String url();
    boolean authenticate() default true; // HTML only.
    PageType pageType() default PageType.HTML;
    boolean digest() default true; // SLAPI only
}
