// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.Arrays;

/**
 * Class generated from EBNF.
 */
@SuppressWarnings("all") // Disable all warnings in the generated file
class Mf2Parser
{
  static class ParseException extends RuntimeException
  {
    private static final long serialVersionUID = 1L;
    private int begin, end, offending, expected, state;

    public ParseException(int b, int e, int s, int o, int x)
    {
      begin = b;
      end = e;
      state = s;
      offending = o;
      expected = x;
    }

    @Override
    public String getMessage()
    {
      return offending < 0
           ? "lexical analysis failed"
           : "syntax error";
    }

    public void serialize(EventHandler eventHandler)
    {
    }

    public int getBegin() {return begin;}
    public int getEnd() {return end;}
    public int getState() {return state;}
    public int getOffending() {return offending;}
    public int getExpected() {return expected;}
    public boolean isAmbiguousInput() {return false;}
  }

  public interface EventHandler
  {
    public void reset(CharSequence string);
    public void startNonterminal(String name, int begin);
    public void endNonterminal(String name, int end);
    public void terminal(String name, int begin, int end);
    public void whitespace(int begin, int end);
  }

  public static class TopDownTreeBuilder implements EventHandler
  {
    private CharSequence input = null;
    public Nonterminal[] stack = new Nonterminal[64];
    private int top = -1;

    @Override
    public void reset(CharSequence input)
    {
      this.input = input;
      top = -1;
    }

    @Override
    public void startNonterminal(String name, int begin)
    {
      Nonterminal nonterminal = new Nonterminal(name, begin, begin, new Symbol[0]);
      if (top >= 0) addChild(nonterminal);
      if (++top >= stack.length) stack = Arrays.copyOf(stack, stack.length << 1);
      stack[top] = nonterminal;
    }

    @Override
    public void endNonterminal(String name, int end)
    {
      stack[top].end = end;
      if (top > 0) --top;
    }

    @Override
    public void terminal(String name, int begin, int end)
    {
      addChild(new Terminal(name, begin, end));
    }

    @Override
    public void whitespace(int begin, int end)
    {
    }

    private void addChild(Symbol s)
    {
      Nonterminal current = stack[top];
      current.children = Arrays.copyOf(current.children, current.children.length + 1);
      current.children[current.children.length - 1] = s;
    }

    public void serialize(EventHandler e)
    {
      e.reset(input);
      stack[0].send(e);
    }
  }

  public static abstract class Symbol
  {
    public String name;
    public int begin;
    public int end;

    protected Symbol(String name, int begin, int end)
    {
      this.name = name;
      this.begin = begin;
      this.end = end;
    }

    public abstract void send(EventHandler e);
  }

  public static class Terminal extends Symbol
  {
    public Terminal(String name, int begin, int end)
    {
      super(name, begin, end);
    }

    @Override
    public void send(EventHandler e)
    {
      e.terminal(name, begin, end);
    }
  }

  public static class Nonterminal extends Symbol
  {
    public Symbol[] children;

    public Nonterminal(String name, int begin, int end, Symbol[] children)
    {
      super(name, begin, end);
      this.children = children;
    }

    @Override
    public void send(EventHandler e)
    {
      e.startNonterminal(name, begin);
      int pos = begin;
      for (Symbol c : children)
      {
        if (pos < c.begin) e.whitespace(pos, c.begin);
        c.send(e);
        pos = c.end;
      }
      if (pos < end) e.whitespace(pos, end);
      e.endNonterminal(name, end);
    }
  }

  public Mf2Parser(CharSequence string, EventHandler t)
  {
    initialize(string, t);
  }

  public void initialize(CharSequence source, EventHandler parsingEventHandler)
  {
    eventHandler = parsingEventHandler;
    input = source;
    size = source.length();
    reset(0, 0, 0);
  }

  public CharSequence getInput()
  {
    return input;
  }

  public int getTokenOffset()
  {
    return b0;
  }

  public int getTokenEnd()
  {
    return e0;
  }

  public final void reset(int l, int b, int e)
  {
            b0 = b; e0 = b;
    l1 = l; b1 = b; e1 = e;
    end = e;
    eventHandler.reset(input);
  }

  public void reset()
  {
    reset(0, 0, 0);
  }

  public static String getOffendingToken(ParseException e)
  {
    return e.getOffending() < 0 ? null : TOKEN[e.getOffending()];
  }

  public static String[] getExpectedTokenSet(ParseException e)
  {
    String[] expected;
    if (e.getExpected() >= 0)
    {
      expected = new String[]{TOKEN[e.getExpected()]};
    }
    else
    {
      expected = getTokenSet(- e.getState());
    }
    return expected;
  }

  public String getErrorMessage(ParseException e)
  {
    String message = e.getMessage();
    String[] tokenSet = getExpectedTokenSet(e);
    String found = getOffendingToken(e);
    int size = e.getEnd() - e.getBegin();
    message += (found == null ? "" : ", found " + found)
            + "\nwhile expecting "
            + (tokenSet.length == 1 ? tokenSet[0] : java.util.Arrays.toString(tokenSet))
            + "\n"
            + (size == 0 || found != null ? "" : "after successfully scanning " + size + " characters beginning ");
    String prefix = input.subSequence(0, e.getBegin()).toString();
    int line = prefix.replaceAll("[^\n]", "").length() + 1;
    int column = prefix.length() - prefix.lastIndexOf('\n');
    return message
         + "at line " + line + ", column " + column + ":\n..."
         + input.subSequence(e.getBegin(), Math.min(input.length(), e.getBegin() + 64))
         + "...";
  }

  public void parse_Message()
  {
    eventHandler.startNonterminal("Message", e0);
    for (;;)
    {
      lookahead1W(12);              // WhiteSpace | 'let' | 'match' | '{'
      if (l1 != 13)                 // 'let'
      {
        break;
      }
      whitespace();
      parse_Declaration();
    }
    switch (l1)
    {
    case 16:                        // '{'
      whitespace();
      parse_Pattern();
      break;
    default:
      whitespace();
      parse_Selector();
      for (;;)
      {
        whitespace();
        parse_Variant();
        lookahead1W(4);             // END | WhiteSpace | 'when'
        if (l1 != 15)               // 'when'
        {
          break;
        }
      }
    }
    eventHandler.endNonterminal("Message", e0);
  }

  private void parse_Declaration()
  {
    eventHandler.startNonterminal("Declaration", e0);
    consume(13);                    // 'let'
    lookahead1W(0);                 // WhiteSpace | Variable
    consume(4);                     // Variable
    lookahead1W(1);                 // WhiteSpace | '='
    consume(12);                    // '='
    lookahead1W(2);                 // WhiteSpace | '{'
    consume(16);                    // '{'
    lookahead1W(9);                 // WhiteSpace | Variable | Function | Literal
    whitespace();
    parse_Expression();
    consume(17);                    // '}'
    eventHandler.endNonterminal("Declaration", e0);
  }

  private void parse_Selector()
  {
    eventHandler.startNonterminal("Selector", e0);
    consume(14);                    // 'match'
    for (;;)
    {
      lookahead1W(2);               // WhiteSpace | '{'
      consume(16);                  // '{'
      lookahead1W(9);               // WhiteSpace | Variable | Function | Literal
      whitespace();
      parse_Expression();
      consume(17);                  // '}'
      lookahead1W(7);               // WhiteSpace | 'when' | '{'
      if (l1 != 16)                 // '{'
      {
        break;
      }
    }
    eventHandler.endNonterminal("Selector", e0);
  }

  private void parse_Variant()
  {
    eventHandler.startNonterminal("Variant", e0);
    consume(15);                    // 'when'
    for (;;)
    {
      lookahead1W(11);              // WhiteSpace | Nmtoken | Literal | '*'
      whitespace();
      parse_VariantKey();
      lookahead1W(13);              // WhiteSpace | Nmtoken | Literal | '*' | '{'
      if (l1 == 16)                 // '{'
      {
        break;
      }
    }
    whitespace();
    parse_Pattern();
    eventHandler.endNonterminal("Variant", e0);
  }

  private void parse_VariantKey()
  {
    eventHandler.startNonterminal("VariantKey", e0);
    switch (l1)
    {
    case 10:                        // Literal
      consume(10);                  // Literal
      break;
    case 9:                         // Nmtoken
      consume(9);                   // Nmtoken
      break;
    default:
      consume(11);                  // '*'
    }
    eventHandler.endNonterminal("VariantKey", e0);
  }

  private void parse_Pattern()
  {
    eventHandler.startNonterminal("Pattern", e0);
    consume(16);                    // '{'
    for (;;)
    {
      lookahead1(8);                // Text | '{' | '}'
      if (l1 == 17)                 // '}'
      {
        break;
      }
      switch (l1)
      {
      case 3:                       // Text
        consume(3);                 // Text
        break;
      default:
        parse_Placeholder();
      }
    }
    consume(17);                    // '}'
    eventHandler.endNonterminal("Pattern", e0);
  }

  private void parse_Placeholder()
  {
    eventHandler.startNonterminal("Placeholder", e0);
    consume(16);                    // '{'
    lookahead1W(14);                // WhiteSpace | Variable | Function | MarkupStart | MarkupEnd | Literal | '}'
    if (l1 != 17)                   // '}'
    {
      switch (l1)
      {
      case 6:                       // MarkupStart
        whitespace();
        parse_Markup();
        break;
      case 7:                       // MarkupEnd
        consume(7);                 // MarkupEnd
        break;
      default:
        whitespace();
        parse_Expression();
      }
    }
    lookahead1W(3);                 // WhiteSpace | '}'
    consume(17);                    // '}'
    eventHandler.endNonterminal("Placeholder", e0);
  }

  private void parse_Expression()
  {
    eventHandler.startNonterminal("Expression", e0);
    switch (l1)
    {
    case 5:                         // Function
      parse_Annotation();
      break;
    default:
      parse_Operand();
      lookahead1W(5);               // WhiteSpace | Function | '}'
      if (l1 == 5)                  // Function
      {
        whitespace();
        parse_Annotation();
      }
    }
    eventHandler.endNonterminal("Expression", e0);
  }

  private void parse_Operand()
  {
    eventHandler.startNonterminal("Operand", e0);
    switch (l1)
    {
    case 10:                        // Literal
      consume(10);                  // Literal
      break;
    default:
      consume(4);                   // Variable
    }
    eventHandler.endNonterminal("Operand", e0);
  }

  private void parse_Annotation()
  {
    eventHandler.startNonterminal("Annotation", e0);
    consume(5);                     // Function
    for (;;)
    {
      lookahead1W(6);               // WhiteSpace | Name | '}'
      if (l1 != 8)                  // Name
      {
        break;
      }
      whitespace();
      parse_Option();
    }
    eventHandler.endNonterminal("Annotation", e0);
  }

  private void parse_Option()
  {
    eventHandler.startNonterminal("Option", e0);
    consume(8);                     // Name
    lookahead1W(1);                 // WhiteSpace | '='
    consume(12);                    // '='
    lookahead1W(10);                // WhiteSpace | Variable | Nmtoken | Literal
    switch (l1)
    {
    case 10:                        // Literal
      consume(10);                  // Literal
      break;
    case 9:                         // Nmtoken
      consume(9);                   // Nmtoken
      break;
    default:
      consume(4);                   // Variable
    }
    eventHandler.endNonterminal("Option", e0);
  }

  private void parse_Markup()
  {
    eventHandler.startNonterminal("Markup", e0);
    consume(6);                     // MarkupStart
    for (;;)
    {
      lookahead1W(6);               // WhiteSpace | Name | '}'
      if (l1 != 8)                  // Name
      {
        break;
      }
      whitespace();
      parse_Option();
    }
    eventHandler.endNonterminal("Markup", e0);
  }

  private void consume(int t)
  {
    if (l1 == t)
    {
      whitespace();
      eventHandler.terminal(TOKEN[l1], b1, e1);
      b0 = b1; e0 = e1; l1 = 0;
    }
    else
    {
      error(b1, e1, 0, l1, t);
    }
  }

  private void whitespace()
  {
    if (e0 != b1)
    {
      eventHandler.whitespace(e0, b1);
      e0 = b1;
    }
  }

  private int matchW(int tokenSetId)
  {
    int code;
    for (;;)
    {
      code = match(tokenSetId);
      if (code != 2)                // WhiteSpace
      {
        break;
      }
    }
    return code;
  }

  private void lookahead1W(int tokenSetId)
  {
    if (l1 == 0)
    {
      l1 = matchW(tokenSetId);
      b1 = begin;
      e1 = end;
    }
  }

  private void lookahead1(int tokenSetId)
  {
    if (l1 == 0)
    {
      l1 = match(tokenSetId);
      b1 = begin;
      e1 = end;
    }
  }

  private int error(int b, int e, int s, int l, int t)
  {
    throw new ParseException(b, e, s, l, t);
  }

  private int     b0, e0;
  private int l1, b1, e1;
  private EventHandler eventHandler = null;
  private CharSequence input = null;
  private int size = 0;
  private int begin = 0;
  private int end = 0;

  private int match(int tokenSetId)
  {
    begin = end;
    int current = end;
    int result = INITIAL[tokenSetId];
    int state = 0;

    for (int code = result & 63; code != 0; )
    {
      int charclass;
      int c0 = current < size ? input.charAt(current) : 0;
      ++current;
      if (c0 < 0x80)
      {
        charclass = MAP0[c0];
      }
      else if (c0 < 0xd800)
      {
        int c1 = c0 >> 4;
        charclass = MAP1[(c0 & 15) + MAP1[(c1 & 31) + MAP1[c1 >> 5]]];
      }
      else
      {
        if (c0 < 0xdc00)
        {
          int c1 = current < size ? input.charAt(current) : 0;
          if (c1 >= 0xdc00 && c1 < 0xe000)
          {
            ++current;
            c0 = ((c0 & 0x3ff) << 10) + (c1 & 0x3ff) + 0x10000;
          }
        }

        int lo = 0, hi = 6;
        for (int m = 3; ; m = (hi + lo) >> 1)
        {
          if (MAP2[m] > c0) {hi = m - 1;}
          else if (MAP2[7 + m] < c0) {lo = m + 1;}
          else {charclass = MAP2[14 + m]; break;}
          if (lo > hi) {charclass = 0; break;}
        }
      }

      state = code;
      int i0 = (charclass << 6) + code - 1;
      code = TRANSITION[(i0 & 7) + TRANSITION[i0 >> 3]];

      if (code > 63)
      {
        result = code;
        code &= 63;
        end = current;
      }
    }

    result >>= 6;
    if (result == 0)
    {
      end = current - 1;
      int c1 = end < size ? input.charAt(end) : 0;
      if (c1 >= 0xdc00 && c1 < 0xe000)
      {
        --end;
      }
      return error(begin, end, state, -1, -1);
    }

    if (end > size) end = size;
    return (result & 31) - 1;
  }

  private static String[] getTokenSet(int tokenSetId)
  {
    java.util.ArrayList<String> expected = new java.util.ArrayList<>();
    int s = tokenSetId < 0 ? - tokenSetId : INITIAL[tokenSetId] & 63;
    for (int i = 0; i < 18; i += 32)
    {
      int j = i;
      int i0 = (i >> 5) * 38 + s - 1;
      int f = EXPECTED[i0];
      for ( ; f != 0; f >>>= 1, ++j)
      {
        if ((f & 1) != 0)
        {
          expected.add(TOKEN[j]);
        }
      }
    }
    return expected.toArray(new String[]{});
  }

  private static final int[] MAP0 =
  {
    /*   0 */ 24, 24, 24, 24, 24, 24, 24, 24, 24, 1, 1, 24, 24, 1, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
    /*  27 */ 24, 24, 24, 24, 24, 1, 24, 24, 24, 2, 24, 24, 24, 3, 4, 5, 6, 24, 7, 8, 24, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
    /*  58 */ 9, 24, 24, 10, 24, 24, 24, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11,
    /*  85 */ 11, 11, 11, 11, 11, 11, 24, 12, 24, 24, 11, 24, 13, 11, 14, 11, 15, 11, 11, 16, 11, 11, 11, 17, 18, 19,
    /* 111 */ 11, 11, 11, 11, 11, 20, 11, 11, 21, 11, 11, 11, 22, 24, 23, 24, 24
  };

  private static final int[] MAP1 =
  {
    /*   0 */ 108, 124, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 156, 181, 181, 181, 181,
    /*  21 */ 181, 214, 215, 213, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
    /*  42 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
    /*  63 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
    /*  84 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
    /* 105 */ 214, 214, 214, 383, 330, 396, 353, 291, 262, 247, 308, 330, 330, 330, 322, 292, 284, 292, 284, 292, 292,
    /* 126 */ 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 347, 347, 347, 347, 347, 347, 347,
    /* 147 */ 277, 292, 292, 292, 292, 292, 292, 292, 292, 369, 330, 330, 331, 329, 330, 330, 292, 292, 292, 292, 292,
    /* 168 */ 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 330, 330, 330, 330, 330, 330, 330, 330,
    /* 189 */ 330, 330, 330, 330, 330, 330, 330, 330, 330, 330, 330, 330, 330, 330, 330, 330, 330, 330, 330, 330, 330,
    /* 210 */ 330, 330, 330, 291, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292,
    /* 231 */ 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 292, 330, 24, 13, 11, 14, 11, 15,
    /* 253 */ 11, 11, 16, 11, 11, 11, 17, 18, 19, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 24, 12, 24, 24, 11, 11,
    /* 279 */ 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 24, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11,
    /* 305 */ 11, 11, 11, 11, 11, 11, 11, 20, 11, 11, 21, 11, 11, 11, 22, 24, 23, 24, 24, 24, 24, 24, 24, 24, 8, 24, 24,
    /* 332 */ 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
    /* 363 */ 9, 24, 24, 10, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 11, 11, 24, 24, 24, 24, 24, 24, 24,
    /* 390 */ 24, 24, 1, 1, 24, 24, 1, 24, 24, 24, 2, 24, 24, 24, 3, 4, 5, 6, 24, 7, 8, 24
  };

  private static final int[] MAP2 =
  {
    /*  0 */ 55296, 63744, 64976, 65008, 65534, 65536, 983040, 63743, 64975, 65007, 65533, 65535, 983039, 1114111, 24,
    /* 15 */ 11, 24, 11, 24, 11, 24
  };

  private static final int[] INITIAL =
  {
    /*  0 */ 1, 2, 3, 4, 133, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
  };

  private static final int[] TRANSITION =
  {
    /*   0 */ 237, 237, 237, 237, 237, 237, 237, 237, 200, 208, 455, 237, 237, 237, 237, 237, 236, 230, 455, 237, 237,
    /*  21 */ 237, 237, 237, 237, 245, 376, 382, 237, 237, 237, 237, 237, 380, 314, 382, 237, 237, 237, 237, 237, 263,
    /*  42 */ 455, 237, 237, 237, 237, 237, 237, 295, 455, 237, 237, 237, 237, 237, 237, 322, 287, 281, 252, 237, 237,
    /*  63 */ 237, 237, 344, 287, 281, 252, 237, 237, 237, 255, 358, 455, 237, 237, 237, 237, 237, 417, 380, 455, 237,
    /*  84 */ 237, 237, 237, 237, 419, 390, 215, 329, 252, 237, 237, 237, 237, 398, 275, 382, 237, 237, 237, 237, 419,
    /* 105 */ 390, 215, 410, 252, 237, 237, 237, 419, 390, 215, 329, 309, 237, 237, 237, 419, 390, 222, 365, 252, 237,
    /* 126 */ 237, 237, 419, 390, 427, 329, 302, 237, 237, 237, 419, 435, 215, 329, 252, 237, 237, 237, 419, 443, 215,
    /* 147 */ 329, 252, 237, 237, 237, 419, 390, 215, 329, 372, 237, 237, 237, 419, 390, 215, 336, 451, 237, 237, 237,
    /* 168 */ 402, 390, 215, 329, 252, 237, 237, 237, 350, 463, 269, 237, 237, 237, 237, 237, 474, 471, 269, 237, 237,
    /* 189 */ 237, 237, 237, 237, 380, 455, 237, 237, 237, 237, 237, 192, 192, 192, 192, 192, 192, 192, 192, 277, 192,
    /* 210 */ 192, 192, 192, 192, 192, 0, 414, 595, 0, 277, 22, 663, 0, 414, 595, 0, 277, 22, 663, 32, 277, 16, 16, 0,
    /* 234 */ 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 277, 22, 22, 22, 0, 22, 22, 0, 482, 547, 0, 0, 0, 0, 0, 18, 0, 0, 277,
    /* 264 */ 0, 0, 768, 0, 768, 0, 0, 0, 277, 0, 22, 0, 0, 0, 277, 20, 31, 0, 0, 0, 348, 0, 414, 0, 0, 595, 0, 277, 22,
    /* 293 */ 663, 0, 277, 0, 0, 0, 0, 0, 26, 0, 482, 547, 0, 0, 960, 0, 0, 482, 547, 0, 38, 0, 0, 0, 0, 277, 704, 0, 0,
    /* 322 */ 277, 0, 663, 663, 0, 663, 27, 0, 482, 547, 348, 0, 414, 0, 0, 482, 547, 348, 0, 414, 0, 896, 277, 0, 663,
    /* 347 */ 663, 0, 663, 0, 0, 1088, 0, 0, 0, 0, 1088, 277, 18, 0, 0, 0, 0, 18, 0, 482, 547, 348, 36, 414, 0, 0, 482,
    /* 374 */ 547, 1024, 0, 0, 0, 0, 277, 0, 0, 0, 0, 0, 0, 0, 22, 0, 277, 0, 663, 663, 0, 663, 0, 348, 20, 0, 0, 0, 0,
    /* 403 */ 0, 0, 0, 17, 0, 595, 17, 33, 482, 547, 348, 0, 414, 0, 0, 832, 0, 0, 0, 0, 0, 0, 595, 0, 29, 414, 595, 0,
    /* 431 */ 277, 22, 663, 0, 277, 0, 663, 663, 24, 663, 0, 348, 277, 0, 663, 663, 25, 663, 0, 348, 37, 482, 547, 0, 0,
    /* 456 */ 0, 0, 0, 277, 22, 0, 0, 1088, 0, 0, 0, 1088, 1088, 0, 0, 1152, 0, 0, 0, 0, 0, 1152, 0, 1152, 1152, 0
  };

  private static final int[] EXPECTED =
  {
    /*  0 */ 20, 4100, 65540, 131076, 32772, 131108, 131332, 98308, 196616, 1076, 1556, 3588, 90116, 69124, 132340, 16,
    /* 16 */ 32768, 32, 256, 8, 8, 1024, 512, 8192, 16384, 64, 128, 16, 32768, 32, 1024, 8192, 16384, 64, 128, 32768,
    /* 36 */ 16384, 16384
  };

  private static final String[] TOKEN =
  {
    "(0)",
    "END",
    "WhiteSpace",
    "Text",
    "Variable",
    "Function",
    "MarkupStart",
    "MarkupEnd",
    "Name",
    "Nmtoken",
    "Literal",
    "'*'",
    "'='",
    "'let'",
    "'match'",
    "'when'",
    "'{'",
    "'}'"
  };
}

// End
