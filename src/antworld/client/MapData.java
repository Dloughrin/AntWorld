package antworld.client;

import antworld.common.LandType;
import antworld.common.Util;

import java.awt.image.BufferedImage;

/**
 * Created by Dustin on 12/10/2016.
 * Class to keep track of water. Wanted to keep track of nests as well, but I get a java heap space error on the AntWorld map
 */
public class MapData
{
  private BufferedImage mapImage;
  public Pixel[][] map;
  private int mapX, mapY;

  public MapData()
  {
    mapImage = Util.loadImage("Small.png", null);
    mapX = mapImage.getWidth();
    mapY = mapImage.getHeight();
    loadMapData();
  }
  private void loadMapData()
  {
    int rgb;
    char contains;
    map = new Pixel[mapX][mapY];

    for(int x = 0; x < mapX; x++)
    {
      for(int y = 0; y < mapY; y++)
      {
        rgb = (mapImage.getRGB(x, y) & 0x00FFFFFF);

        if(rgb == LandType.WATER.getMapColor())
        {
          contains = 'w'; //water
        }
        else
        {
          contains = 'l'; //land
        }

        map[x][y] = new Pixel(x,y,contains);
      }
    }
  }
}
