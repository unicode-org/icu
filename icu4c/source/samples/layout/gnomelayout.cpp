/*
 ****************************************************************************** *
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 ****************************************************************************** *
 *   file name:  gnomelayout.cpp
 *
 *   created on: 09/04/2001
 *   created by: Eric R. Mader
 */

#include <gnome.h>
#include "freetype/freetype.h"

#include "unicode/ustring.h"
#include "unicode/uscript.h"

#include "unicode/loengine.h"

#include "GnomeFontInstance.h"

#include "paragraph.h"

#include "GnomeGUISupport.h"
#include "GnomeFontMap.h"
#include "UnicodeReader.h"
#include "scrptrun.h"

#define ARRAY_LENGTH(array) (sizeof array / sizeof array[0])

void showabout(GtkWidget *widget, gpointer data)
{
    GtkWidget *aboutBox;
    const gchar *writtenBy[] = {
        "Eric Mader",
        NULL
    };

    aboutBox = gnome_about_new("Gnome Layout Demo",
                               "0.1",
                               "Copyright (C) 1998-2001 By International Business Machines Corporation and others. All Rights Reserved.",
                               writtenBy,
                               "A simple demo of the ICU LayoutEngine.",
                               NULL);

    gtk_widget_show(aboutBox);
}

void notimpl(GtkObject *object, gpointer data)
{
    gnome_ok_dialog("Not implemented...");
}

void shutdown(GtkObject *object, gpointer data)
{
    gtk_main_quit();
}

GnomeUIInfo fileMenu[] =
{
    GNOMEUIINFO_MENU_OPEN_ITEM(notimpl, NULL),
    GNOMEUIINFO_SEPARATOR,
    GNOMEUIINFO_MENU_EXIT_ITEM(shutdown, NULL),
    GNOMEUIINFO_END
};

GnomeUIInfo helpMenu[] =
{
    // GNOMEUIINFO_HELP("gnomelayout"),
    GNOMEUIINFO_MENU_ABOUT_ITEM(showabout, NULL),
    GNOMEUIINFO_END
};

GnomeUIInfo mainMenu[] =
{
    GNOMEUIINFO_SUBTREE(N_("File"), fileMenu),
    GNOMEUIINFO_SUBTREE(N_("Help"), helpMenu),
    GNOMEUIINFO_END
};

struct Context
{
    long width;
    long height;
    Paragraph *paragraph;
};

gint eventDelete(GtkWidget *widget, GdkEvent *event, gpointer data)
{
    return FALSE;
}

gint eventDestroy(GtkWidget *widget, GdkEvent *event, Context *context)
{
    shutdown(GTK_OBJECT(widget), context);
    return 0;
}

gint eventConfigure(GtkWidget *widget, GdkEventConfigure *event, Context *context)
{
    context->width  = event->width;
    context->height = event->height;

    if (context->width > 0 && context->height > 0) {
        context->paragraph->breakLines(context->width, context->height);
    }

    return TRUE;
}

gint eventExpose(GtkWidget *widget, GdkEvent *event, Context *context)
{
    gint maxLines = context->paragraph->getLineCount() - 1;
    gint firstLine = 0, lastLine = context->height / context->paragraph->getLineHeight();

    context->paragraph->draw(widget, firstLine, (maxLines < lastLine)? maxLines : lastLine);

    return TRUE;
}

int main (int argc, char *argv[])
{
    GtkWidget *app;
    GtkWidget *area;
    GtkStyle  *style;
    unsigned short status = 0;
    Context context = {600, 400, NULL};
    TT_Engine engine;

    TT_Init_FreeType(&engine);

    RFIErrorCode     fontStatus = RFI_NO_ERROR;
    GnomeGUISupport *guiSupport = new GnomeGUISupport();
    GnomeFontMap    *fontMap    = new GnomeFontMap(engine, "FontMap.Gnome", 24, guiSupport, fontStatus);

    if (LE_FAILURE(fontStatus)) {
        TT_Done_FreeType(engine);
        return 1;
    }

    // FIXME: is it cheating to pass NULL for surface, since we know that
    // GnomeFontInstance won't use it?
    context.paragraph = Paragraph::paragraphFactory("Sample.txt", fontMap, guiSupport, NULL);

    if (context.paragraph != NULL) {
        gnome_init("gnomelayout", "1.0", argc, argv);
        app = gnome_app_new("gnomelayout", "Gnome Layout");

        gtk_window_set_default_size(GTK_WINDOW(app), 600 - 24, 400);

        gnome_app_create_menus(GNOME_APP(app), mainMenu);

        gtk_signal_connect(GTK_OBJECT(app),
                           "delete_event",
                           GTK_SIGNAL_FUNC(eventDelete),
                           NULL);

        gtk_signal_connect(GTK_OBJECT(app),
                           "destroy",
                           GTK_SIGNAL_FUNC(eventDestroy),
                           &context);

        area = gtk_drawing_area_new();

#if 1
        style = gtk_style_copy(gtk_widget_get_style(area));

        for (int i = 0; i < 5; i += 1) {
            style->fg[i] =style->white;
        }
    
        gtk_widget_set_style(area, style);
#endif

        gnome_app_set_contents(GNOME_APP(app), area);

        gtk_signal_connect(GTK_OBJECT(area),
                           "expose_event",
                           GTK_SIGNAL_FUNC(eventExpose),
                           &context);

        gtk_signal_connect(GTK_OBJECT(area),
                           "configure_event",
                           GTK_SIGNAL_FUNC(eventConfigure),
                           &context);

        gtk_widget_show_all(app);

        gtk_main();

        delete context.paragraph;
    }

    delete fontMap;

    TT_Done_FreeType(engine);

    exit(0);
}
