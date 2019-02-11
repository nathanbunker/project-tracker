package org.openimmunizationsoftware.pt.manager;

public class MoneyUtil
{
  public static String format(int i)
  {
    if (i == 0)
    {
      return "0.00";
    }
    int cents = Math.abs(i % 100);
    String valueCents = ("0" + cents);
    if (valueCents.length() > 2)
    {
      valueCents = valueCents.substring(1);
    }
    int dollars = (int) (i / 100);
    String valueDollars = String.valueOf(dollars);
    if (dollars < 0)
    {
      if (valueDollars.length() > 4)
      {
        int pos = valueDollars.length() - 3;
        valueDollars = valueDollars.substring(0, pos) + "," + valueDollars.substring(pos);
      }
      return valueDollars + "." + valueCents;
    } else
    {
      if (valueDollars.length() > 3)
      {
        int pos = valueDollars.length() - 3;
        valueDollars = valueDollars.substring(0, pos) + "," + valueDollars.substring(pos);
      }
      return valueDollars + "." + valueCents;
    }
  }

  public static int parse(String s)
  {
    int pos = s.indexOf(",");
    while (pos != -1)
    {
      s = s.substring(0, pos) + s.substring(pos +1);
      pos = s.indexOf(",");
    }
    int value = 0;
    pos = s.indexOf(".");
    if (pos == -1)
    {
      value = Integer.parseInt(s + "00");
    } else
    {
      if ((pos + 2) == s.length())
      {
        s = s.substring(0, pos) + s.substring(pos + 1);
      } else if ((pos + 2) < s.length())
      {
        s = s.substring(0, pos) + s.substring(pos + 1, pos + 3);
      } else if ((pos + 2) > s.length())
      {
        s = s.substring(0, pos) + s.substring(pos + 1, pos + 2) + "0";
      } else
      {
        s = s.substring(0, pos) + "00";
      }
      value = Integer.parseInt(s);
    }

    return value;
  }
}
