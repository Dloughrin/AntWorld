package antworld.client;

/**
 * Created by Dustin on 12/10/2016.
 */
public class Pixel
{
  public int x,y;
  public char contains;

  public Pixel(int x, int y, char type)
  {
    this.x = x;
    this.y = y;
    contains = type;
  }
}
