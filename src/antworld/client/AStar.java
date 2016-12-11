package antworld.client;

/**
 * Created by Dustin on 12/10/2016.
 */
public class AStar
{
  public static int manhattanDistance(int startX, int startY, int targetX, int targetY)
  {
    int dx = Math.abs(targetX - startX);
    int dy = Math.abs(targetY - startY);
    return dx + dy;
  }
}
