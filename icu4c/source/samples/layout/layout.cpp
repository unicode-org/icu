/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  Layout.cpp
 *
 *   created on: 08/03/2000
 *   created by: Eric R. Mader
 */

#include <windows.h>

//#include "LETypes.h"
//#include "LEFontInstance.h"
//#include "LayoutEngine.h"
//#include "unicode/loengine.h"
#include "unicode/uscript.h"
//#include "LEScripts.h"

#include "GDIFontInstance.h"

#include "paragraph.h"

#include "GDIGUISupport.h"
#include "GDIFontMap.h"
#include "UnicodeReader.h"
#include "scrptrun.h"

#define ARRAY_LENGTH(array) (sizeof array / sizeof array[0])

LRESULT CALLBACK WndProc(HWND, UINT, WPARAM, LPARAM);

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, PSTR szCmdLine, int iCmdShow)
{
    HWND hwnd;
    MSG msg;
    WNDCLASS wndclass;
    TCHAR szAppName[] = TEXT("LayoutDemo");
    TCHAR szTitle[] = TEXT("LayoutDemo: Demo of LayoutEngine");
    RFIErrorCode status = RFI_NO_ERROR;

    wndclass.style = CS_HREDRAW | CS_VREDRAW;
    wndclass.lpfnWndProc = WndProc;
    wndclass.cbClsExtra = 0;
    wndclass.cbWndExtra = sizeof(LONG);
    wndclass.hInstance = hInstance;
    wndclass.hIcon = LoadIcon(NULL, IDI_APPLICATION);
    wndclass.hCursor = LoadCursor(NULL, IDC_ARROW);
    wndclass.hbrBackground = (HBRUSH) GetStockObject(WHITE_BRUSH);
    wndclass.lpszMenuName = NULL;
    wndclass.lpszClassName = szAppName;

    if (!RegisterClass(&wndclass)) {
        MessageBox(NULL, TEXT("This demo only runs on Windows 2000!"), szAppName, MB_ICONERROR);

        return 0;
    }

    hwnd = CreateWindow(szAppName, szTitle,
        WS_OVERLAPPEDWINDOW | WS_VSCROLL,
        CW_USEDEFAULT, CW_USEDEFAULT,
        600, 400,
        NULL, NULL, hInstance, NULL);

    ShowWindow(hwnd, iCmdShow);
    UpdateWindow(hwnd);

    while (GetMessage(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }

    return msg.wParam;
}

LRESULT CALLBACK WndProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam)
{
    HDC hdc;
    Paragraph *paragraph;
    static le_int32 windowCount = 0;
    static GDIFontMap *fontMap = NULL;
    static GDIGUISupport *guiSupport = new GDIGUISupport();

    switch (message) {
    case WM_CREATE:
    {
        RFIErrorCode fontStatus = RFI_NO_ERROR;

        hdc = GetDC(hwnd);

        fontMap = new GDIFontMap(hdc, "FontMap.GDI", 24, guiSupport, fontStatus);

        if (LE_FAILURE(fontStatus)) {
            ReleaseDC(hwnd, hdc);
            return 0;
        }

        paragraph = Paragraph::paragraphFactory("Sample.txt", fontMap, guiSupport, hdc);
        SetWindowLong(hwnd, 0, (LONG) paragraph);

        windowCount += 1;
        ReleaseDC(hwnd, hdc);
        return 0;
    }

    case WM_SIZE:
    {
        le_int32 width = LOWORD(lParam);
        le_int32 height = HIWORD(lParam);
        SCROLLINFO si;

        
        paragraph = (Paragraph *) GetWindowLong(hwnd, 0);

        if (paragraph != NULL) {
            // FIXME: does it matter what we put in the ScrollInfo
            // if the window's been minimized?
            if (width > 0 && height > 0) {
                paragraph->breakLines(width, height);
            }

            si.cbSize = sizeof si;
            si.fMask = SIF_RANGE | SIF_PAGE | SIF_DISABLENOSCROLL;
            si.nMin = 0;
            si.nMax = paragraph->getLineCount() - 1;
            si.nPage = height / paragraph->getLineHeight();
            SetScrollInfo(hwnd, SB_VERT, &si, true);
        }

        return 0;
    }

    case WM_VSCROLL:
    {
        SCROLLINFO si;
        le_int32 vertPos;

        si.cbSize = sizeof si;
        si.fMask = SIF_ALL;
        GetScrollInfo(hwnd, SB_VERT, &si);

        vertPos = si.nPos;

        switch (LOWORD(wParam))
        {
        case SB_TOP:
            si.nPos = si.nMin;
            break;

        case SB_BOTTOM:
            si.nPos = si.nMax;
            break;

        case SB_LINEUP:
            si.nPos -= 1;
            break;

        case SB_LINEDOWN:
            si.nPos += 1;
            break;

        case SB_PAGEUP:
            si.nPos -= si.nPage;
            break;

        case SB_PAGEDOWN:
            si.nPos += si.nPage;
            break;

        case SB_THUMBTRACK:
            si.nPos = si.nTrackPos;
            break;

        default:
            break;
        }

        si.fMask = SIF_POS;
        SetScrollInfo(hwnd, SB_VERT, &si, true);
        GetScrollInfo(hwnd, SB_VERT, &si);

        paragraph = (Paragraph *) GetWindowLong(hwnd, 0);

        if (paragraph != NULL && si.nPos != vertPos) {
            ScrollWindow(hwnd, 0, paragraph->getLineHeight() * (vertPos - si.nPos), NULL, NULL);
            UpdateWindow(hwnd);
        }

        return 0;
    }

    case WM_PAINT:
    {
        PAINTSTRUCT ps;
        SCROLLINFO si;
        le_int32 firstLine, lastLine;

        hdc = BeginPaint(hwnd, &ps);

        si.cbSize = sizeof si;
        si.fMask = SIF_ALL;
        GetScrollInfo(hwnd, SB_VERT, &si);

        firstLine = si.nPos;

        paragraph = (Paragraph *) GetWindowLong(hwnd, 0);

        if (paragraph != NULL) {
            // NOTE: si.nPos + si.nPage may include a partial line at the bottom
            // of the window. We need this because scrolling assumes that the
            // partial line has been painted.
            lastLine  = min (si.nPos + (le_int32) si.nPage, paragraph->getLineCount() - 1);

            paragraph->draw(hdc, firstLine, lastLine);
        }

        EndPaint(hwnd, &ps);
        return 0;
    }

    case WM_DESTROY:
    {
        paragraph = (Paragraph *) GetWindowLong(hwnd, 0);

        if (paragraph != NULL) {
            delete paragraph;
        }

        if (--windowCount <= 0) {
            delete fontMap;

            PostQuitMessage(0);
        }

        return 0;
    }

    default:
        return DefWindowProc(hwnd, message, wParam, lParam);
    }

    return 0;
}
