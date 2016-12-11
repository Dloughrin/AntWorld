package antworld.common;

public enum Direction 
{ NORTH
  { public int deltaX() {return  0;}
    public int deltaY() {return -1;}
  }, 
  
  NORTHEAST
  { public int deltaX() {return  1;}
    public int deltaY() {return -1;}
  },
  
  EAST
  { public int deltaX() {return  1;}
    public int deltaY() {return  0;}
  },

  SOUTHEAST
  { public int deltaX() {return  1;}
    public int deltaY() {return  1;}
  },

  SOUTH
  { public int deltaX() {return  0;}
    public int deltaY() {return  1;}
  },
  
  SOUTHWEST
  { public int deltaX() {return -1;}
    public int deltaY() {return  1;}
  },
  
  WEST
  { public int deltaX() {return -1;}
    public int deltaY() {return  0;}
  },
 
  NORTHWEST
  { public int deltaX() {return -1;}
    public int deltaY() {return -1;}
  };
  

  public abstract int deltaX();
  public abstract int deltaY();
  public static final int SIZE = values().length;
  public static final Direction getDirection(int dx, int dy)
  {

    if(dx == 0 && dy < 0) return NORTH;
    if(dx > 0 && dy < 0) return NORTHEAST;
    if(dx > 0 && dy == 0) return EAST;
    if(dx > 0 && dy > 0) return SOUTHEAST;
    if(dx == 0 && dy > 0) return SOUTH;
    if(dx < 0 && dy > 0) return SOUTHWEST;
    if(dx < 0 && dy == 0) return WEST;
    else return NORTHWEST;



  }
  public static final Direction getRandomDir() {return values()[Constants.random.nextInt(SIZE)];}
  public static final Direction getLeftDir(Direction dir) {return values()[(dir.ordinal()+SIZE-1) % SIZE];}
  public static final Direction getRightDir(Direction dir) {return values()[(dir.ordinal()+1) % SIZE];}
}
