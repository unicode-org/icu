package com.ibm.richtext.textapps.resources;

import java.util.ListResourceBundle;

public final class Sample extends ListResourceBundle {
    
    public Object[][] getContents() {
        
        return new Object[][] {
            { "default.sample", this.getClass().getResource("hagan") },
            { "default.name", "The Garden" },
            { "japanese.sample", "\u6ce8: {1} \u306e\u30e1\u30bd\u30c3\u30c9 {0} \u306f\u63a8\u5968\u3055\u308c\u307e\u305b\u3093\u3002" },
            { "japanese.name", "Japanese Message" },
        };
    }
}