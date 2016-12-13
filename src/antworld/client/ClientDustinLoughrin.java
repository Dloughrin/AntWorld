package antworld.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import antworld.common.*;
import antworld.common.AntAction.AntActionType;
import antworld.server.Ant;
import antworld.server.Cell;

public class ClientDustinLoughrin
{
  private static final boolean DEBUG = false;
  private final TeamNameEnum myTeam;
  private static final long password = 962740848319L;//Each team has been assigned a random password.
  private ObjectInputStream inputStream = null;
  private ObjectOutputStream outputStream = null;
  private boolean isConnected = false;
  private NestNameEnum myNestName = null;
  private int centerX, centerY;

  private MapData localMapData = new MapData();
  private Pixel localMap[][] = localMapData.map;

  private Socket clientSocket;


  //A random number generator is created in Constants. Use it.
  //Do not create a new generator every time you want a random number nor
  //  even in every class were you want a generator.
  private static Random random = Constants.random;


  public ClientDustinLoughrin(String host, int portNumber, TeamNameEnum team)
  {
    myTeam = team;
    System.out.println("Starting " + team +" on " + host + ":" + portNumber + " at "
      + System.currentTimeMillis());

    isConnected = openConnection(host, portNumber);
    if (!isConnected) System.exit(0);
    CommData data = obtainNest();
    mainGameLoop(data);
    closeAll();
  }

  private boolean openConnection(String host, int portNumber)
  {
    try
    {
      clientSocket = new Socket(host, portNumber);
    }
    catch (UnknownHostException e)
    {
      System.err.println("ClientDustinLoughrin Error: Unknown Host " + host);
      e.printStackTrace();
      return false;
    }
    catch (IOException e)
    {
      System.err.println("ClientDustinLoughrin Error: Could not open connection to " + host + " on port " + portNumber);
      e.printStackTrace();
      return false;
    }

    try
    {
      outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
      inputStream = new ObjectInputStream(clientSocket.getInputStream());

    }
    catch (IOException e)
    {
      System.err.println("ClientDustinLoughrin Error: Could not open i/o streams");
      e.printStackTrace();
      return false;
    }

    return true;

  }

  public void closeAll()
  {
    System.out.println("ClientDustinLoughrin.closeAll()");
    {
      try
      {
        if (outputStream != null) outputStream.close();
        if (inputStream != null) inputStream.close();
        clientSocket.close();
      }
      catch (IOException e)
      {
        System.err.println("ClientDustinLoughrin Error: Could not close");
        e.printStackTrace();
      }
    }
  }

  /**
   * This method is called ONCE after the socket has been opened.
   * The server assigns a nest to this client with an initial ant population.
   * @return a reusable CommData structure populated by the server.
   */
  public CommData obtainNest()
  {
      CommData data = new CommData(myTeam);
      data.password = password;

      if( sendCommData(data) )
      {
        try
        {
          if (DEBUG) System.out.println("ClientDustinLoughrin: listening to socket....");
          data = (CommData) inputStream.readObject();
          if (DEBUG) System.out.println("ClientDustinLoughrin: received <<<<<<<<<"+inputStream.available()+"<...\n" + data);
          
          if (data.errorMsg != null)
          {
            System.err.println("ClientDustinLoughrin***ERROR***: " + data.errorMsg);
            System.exit(0);
          }
        }
        catch (IOException e)
        {
          System.err.println("ClientDustinLoughrin***ERROR***: client read failed");
          e.printStackTrace();
          System.exit(0);
        }
        catch (ClassNotFoundException e)
        {
          System.err.println("ClientDustinLoughrin***ERROR***: client sent incorrect common format");
        }
      }
    if (data.myTeam != myTeam)
    {
      System.err.println("ClientDustinLoughrin***ERROR***: Server returned wrong team name: "+data.myTeam);
      System.exit(0);
    }
    if (data.myNest == null)
    {
      System.err.println("ClientDustinLoughrin***ERROR***: Server returned NULL nest");
      System.exit(0);
    }

    myNestName = data.myNest;
    centerX = data.nestData[myNestName.ordinal()].centerX;
    centerY = data.nestData[myNestName.ordinal()].centerY;
    System.out.println("ClientDustinLoughrin: ==== Nest Assigned ===>: " + myNestName);
    return data;
  }
    
  public void mainGameLoop(CommData data)
  {
    while (true)
    { 
      try
      {

        if (DEBUG) System.out.println("ClientDustinLoughrin: chooseActions: " + myNestName);

        chooseActionsOfAllAnts(data);  

        CommData sendData = data.packageForSendToServer();
        
        System.out.println("ClientDustinLoughrin: Sending>>>>>>>: " + sendData);
        outputStream.writeObject(sendData);
        outputStream.flush();
        outputStream.reset();
       

        if (DEBUG) System.out.println("ClientDustinLoughrin: listening to socket....");
        CommData receivedData = (CommData) inputStream.readObject();
        if (DEBUG) System.out.println("ClientDustinLoughrin: received <<<<<<<<<"+inputStream.available()+"<...\n" + receivedData);
        data = receivedData;
  
        
        
        if ((myNestName == null) || (data.myTeam != myTeam))
        {
          System.err.println("ClientDustinLoughrin: !!!!ERROR!!!! " + myNestName);
        }
      }
      catch (IOException e)
      {
        System.err.println("ClientDustinLoughrin***ERROR***: client read failed");
        e.printStackTrace();
        System.exit(0);

      }
      catch (ClassNotFoundException e)
      {
        System.err.println("ServerToClientConnection***ERROR***: client sent incorrect common format");
        e.printStackTrace();
        System.exit(0);
      }

    }
  }
  
  
  private boolean sendCommData(CommData data)
  {
    
    CommData sendData = data.packageForSendToServer();
    try
    {
      if (DEBUG) System.out.println("ClientDustinLoughrin.sendCommData(" + sendData +")");
      outputStream.writeObject(sendData);
      outputStream.flush();
      outputStream.reset();
    }
    catch (IOException e)
    {
      System.err.println("ClientDustinLoughrin***ERROR***: client read failed");
      e.printStackTrace();
      System.exit(0);
    }

    return true;
    
  }

  private void chooseActionsOfAllAnts(CommData commData)
  {
    ArrayList<AntData> newAnts = new ArrayList<>();
    for (AntData ant : commData.myAntList)
    {
      AntAction action;
      if(ant.myAction.type == AntActionType.BIRTH) {}
      else
      {
        action = chooseAction(commData, ant);
        ant.myAction = action;
      }
      birthAnt(ant,commData, newAnts);
    }
    commData.myAntList.addAll(newAnts);
  }




  //=============================================================================
  // This method sets the given action to EXIT_NEST if and only if the given
  //   ant is underground.
  // Returns true if an action was set. Otherwise returns false
  //=============================================================================
  private boolean exitNest(AntData ant, AntAction action)
  {
    if(ant.antType == AntType.DEFENCE || ant.antType == AntType.ATTACK) return false;

    if (ant.underground)
    {
      action.type = AntActionType.EXIT_NEST;
      action.x = centerX - (Constants.NEST_RADIUS-1) + random.nextInt(2 * (Constants.NEST_RADIUS-1));
      action.y = centerY - (Constants.NEST_RADIUS-1) + random.nextInt(2 * (Constants.NEST_RADIUS-1));
      return true;
    }
    return false;
  }


  private boolean attackAdjacent(AntData ant, AntAction action, AntData enemy)
  {
    int x, y;
    Pixel neighbor;

    for (Direction direction : Direction.values())
    {
      x = ant.gridX + direction.deltaX();
      y = ant.gridY + direction.deltaY();
      neighbor = localMap[x][y];

      if(neighbor == null) continue;

      if(neighbor.contains == 'w') continue;

      if(enemy.teamName == ant.teamName) continue;

      if(neighbor.x == enemy.gridX && neighbor.y == enemy.gridY)
      {
        action.type = AntActionType.ATTACK;
        action.direction = direction;
        return true;
      }
    }
    return false;
  }

  private boolean pickUpFoodAdjacent(AntData ant, AntAction action, FoodData food)
  {
    int x, y;
    Pixel neighbor;

    if(ant.carryUnits > 0)
    {
      if(ant.carryUnits >= ant.antType.getCarryCapacity()) return false;
      if(ant.carryType == FoodType.WATER)
      {
        action.direction = Direction.NORTH;
        action.type = AntActionType.DROP;
        action.quantity = ant.carryUnits;
        return true;
      }
    }

    for(Direction direction : Direction.values())
    {
      if(direction == Direction.NORTHEAST || direction ==  Direction.NORTHWEST ||
        direction == Direction.SOUTHEAST || direction == Direction.SOUTHWEST)
      {
        continue;
      }
      x = ant.gridX + direction.deltaX();
      y = ant.gridY + direction.deltaY();
      neighbor = localMap[x][y];

      if(neighbor == null) continue;

      if(neighbor.contains == 'w') continue;

      if(neighbor.x == food.gridX && neighbor.y == food.gridY)
      {
        action.type = AntActionType.PICKUP;
        action.direction = direction;

        if(ant.antType.getCarryCapacity()/2 > (ant.carryUnits + food.getCount()))
        {
          action.quantity = food.getCount();
          return true;
        }
        else
        {
          action.quantity = (ant.antType.getCarryCapacity() - ant.carryUnits)/2;
          return true;
        }
      }
    }

    return false;
  }

  private boolean goHomeIfCarryingOrHurt(AntData ant, AntAction action, CommData data)
  {
    if((ant.carryUnits > 0
            || (ant.health < ((double)ant.antType.getMaxHealth()/2.0))))
    {
      return goHome(ant, action,data);
    }
    return false;
  }
  private boolean goHome(AntData ant, AntAction action, CommData data)
  {
    if(ant.underground && (ant.health < ant.antType.getMaxHealth()))
    {
      return healAtNest(ant, action);
    }
    if(ant.underground && (ant.carryUnits > 0))
    {
      return dropOffLoadAtNest(ant, action);
    }

    int nestDistance = AStar.manhattanDistance(ant.gridX,ant.gridY,centerX,centerY);
    if(localMap[ant.gridX][ant.gridY].contains == 'n' && nestDistance <= Constants.NEST_RADIUS)
    {
      action.type = AntActionType.ENTER_NEST;
      return true;
    }

    int dx = centerX - ant.gridX ;
    int dy = centerY - ant.gridY ;
    int x, y;
    Direction direction = Direction.getDirection(dx,dy);
    action.type = AntActionType.MOVE;
    action.direction = direction;

    for(AntData ants : data.myAntList)
    {
      x = ant.gridX + action.direction.deltaX();
      y = ant.gridY + action.direction.deltaY();

      if(ants.gridX == x && ants.gridY == y)
      {
        action.direction = Direction.getLeftDir(action.direction);
      }
    }

    return true;
  }

  private boolean dropOffLoadAtNest(AntData ant, AntAction action)
  {
    if(!ant.underground) return false;

    action.direction = Direction.NORTH;
    action.type = AntActionType.DROP;
    action.quantity = ant.carryUnits;

    return true;
  }

  private boolean healAtNest(AntData ant, AntAction action)
  {
    if(!ant.underground) return false;

    action.type = AntActionType.HEAL;

    return true;
  }

  private boolean pickUpWater(AntData ant, AntAction action)
  {
    int x,y;

    for(Direction direction : Direction.values())
    {
      x = ant.gridX + direction.deltaX();
      y = ant.gridY + direction.deltaY();
      Pixel neighbor = localMap[x][y];

      if(neighbor.contains == 'w'&& (ant.carryUnits < ((ant.antType.getCarryCapacity()/2)-1)))
      {
        action.type = AntActionType.PICKUP;
        action.direction = direction;
        action.quantity = ((ant.antType.getCarryCapacity() - ant.carryUnits)/2)-1;

        return true;
      }
    }

    return false;
  }

  private boolean goToEnemyAnt(AntData ant, AntAction action, CommData data)
  {
    int distance, lowestDistance = -1;
    int dx, dy;

    if(ant.carryUnits > 0) return false;
    if(ant.antType ==  AntType.MEDIC) return false;
    if(ant.underground)
    {
      action.direction = Direction.NORTH;
      action.type = AntActionType.EXIT_NEST;
      return true;
    }

    for(AntData enemy : data.enemyAntSet)
    {
      distance = AStar.manhattanDistance(ant.gridX,ant.gridY,enemy.gridX,enemy.gridY);
      if(distance < lowestDistance || lowestDistance == -1 )
      {
        if(!(distance < ant.antType.getVisionRadius()*2)
                && !(ant.antType == AntType.ATTACK) && !(ant.antType == AntType.DEFENCE))
        {
          return false;
        }
        lowestDistance = distance;
        if(distance <= 1) return attackAdjacent(ant,action,enemy);
        dx = enemy.gridX - ant.gridX;
        dy = enemy.gridY - ant.gridY;
        action.type = AntActionType.MOVE;
        action.direction = Direction.getDirection(dx,dy);
        return true;
      }
    }
    return false;
  }

  private boolean goToFood(AntData ant, AntAction action, CommData data)
  {
    int distanceToFood;
    if(ant.antType == AntType.ATTACK || ant.antType == AntType.DEFENCE) return false;
    if(!data.foodSet.isEmpty())
    {
      for (FoodData food : data.foodSet)
      {
        distanceToFood = AStar.manhattanDistance(ant.gridX,ant.gridY,food.gridX,food.gridY);
        if(distanceToFood < 2)
        {
          return pickUpFoodAdjacent(ant,action,food);
        }

        if(distanceToFood < ant.antType.getVisionRadius()*2)
        {
          if(!(ant.carryType == FoodType.WATER))
          {
            int dx = food.gridX - ant.gridX  ;
            int dy = food.gridY - ant.gridY ;
            action.type = AntActionType.MOVE;
            action.direction = Direction.getDirection(dx,dy);

            for(AntData ants : data.myAntList)
            {
              int x = ant.gridX + action.direction.deltaX();
              int y = ant.gridY + action.direction.deltaY();

              if(ants.gridX == x && ants.gridY == y)
              {
                action.direction = Direction.getLeftDir(action.direction);
              }
            }
            return true;
          }
          return false;
        }
      }
    }

    return false;
  }

  private void birthAnt(AntData ant, CommData data, ArrayList<AntData> myAntList)
  {
    int minFood = 200;
    int currentFood;
    int antListSize = data.myAntList.size();
    int foodAmount = data.foodStockPile[FoodType.SEEDS.ordinal()] +
            data.foodStockPile[FoodType.NECTAR.ordinal()] + data.foodStockPile[FoodType.MEAT.ordinal()];
    if(antListSize < 100 && foodAmount >= AntType.TOTAL_FOOD_UNITS_TO_SPAWN)
    {
      if (data.foodStockPile[FoodType.SEEDS.ordinal()] >= AntType.TOTAL_FOOD_UNITS_TO_SPAWN)
      {
        currentFood = data.foodStockPile[FoodType.SEEDS.ordinal()];
        for (; (antListSize < 100) && (currentFood >= AntType.TOTAL_FOOD_UNITS_TO_SPAWN); )
        {
          AntData newAnt = new AntData(ant);
          newAnt.antType = AntType.WORKER;
          myAntList.add(newAnt);
          antListSize++;
          currentFood =- AntType.TOTAL_FOOD_UNITS_TO_SPAWN;
        }
      }
      if (data.foodStockPile[FoodType.NECTAR.ordinal()] >= AntType.TOTAL_FOOD_UNITS_TO_SPAWN)
      {
        currentFood = data.foodStockPile[FoodType.NECTAR.ordinal()];
        for (; (antListSize < 100) && (currentFood >= AntType.TOTAL_FOOD_UNITS_TO_SPAWN); )
        {
          AntData newAnt = new AntData(ant);
          newAnt.antType = AntType.SPEED;
          myAntList.add(newAnt);
          antListSize++;
          currentFood =- AntType.TOTAL_FOOD_UNITS_TO_SPAWN;
        }
      }
      if (data.foodStockPile[FoodType.MEAT.ordinal()] >= AntType.TOTAL_FOOD_UNITS_TO_SPAWN)
      {
        currentFood = data.foodStockPile[FoodType.MEAT.ordinal()];
        for (; (antListSize < 100) && (currentFood >= AntType.TOTAL_FOOD_UNITS_TO_SPAWN); )
        {
          AntData newAnt = new AntData(ant);
          newAnt.antType = AntType.ATTACK;
          myAntList.add(newAnt);
          antListSize++;
          currentFood =- AntType.TOTAL_FOOD_UNITS_TO_SPAWN;
        }
      }
    }
    if (data.foodStockPile[FoodType.SEEDS.ordinal()] > minFood)
    {
      currentFood = data.foodStockPile[FoodType.SEEDS.ordinal()];
      for (; (currentFood >= minFood); )
      {
        AntData newAnt = new AntData(ant);
        newAnt.antType = AntType.WORKER;
        myAntList.add(newAnt);
        currentFood =- AntType.TOTAL_FOOD_UNITS_TO_SPAWN;
      }
    }
    if (data.foodStockPile[FoodType.NECTAR.ordinal()] > minFood)
    {
      currentFood = data.foodStockPile[FoodType.NECTAR.ordinal()];
      for (; currentFood >= minFood; )
      {
        AntData newAnt = new AntData(ant);
        newAnt.antType = AntType.SPEED;
        myAntList.add(newAnt);
        currentFood =- AntType.TOTAL_FOOD_UNITS_TO_SPAWN;
      }
    }
    if (data.foodStockPile[FoodType.MEAT.ordinal()] > minFood)
    {
      currentFood = data.foodStockPile[FoodType.MEAT.ordinal()];
      for (; (currentFood >= minFood); )
      {
        AntData newAnt = new AntData(ant);
        newAnt.antType = AntType.ATTACK;
        myAntList.add(newAnt);
        currentFood =- AntType.TOTAL_FOOD_UNITS_TO_SPAWN;
      }
    }
  }

  private boolean goExplore(AntData ant, AntAction action, CommData data)
  {
    if(ant.antType == AntType.DEFENCE || ant.antType == AntType.ATTACK) return false;
    int dx = ant.gridX - centerX;
    int dy = ant.gridY - centerY;
    Direction direction = Direction.getDirection(dx,dy);
    action.type = AntActionType.MOVE;
    action.direction = direction;

    for(AntData ants : data.myAntList)
    {
      int x = ant.gridX + direction.deltaX();
      int y = ant.gridY + direction.deltaY();

      if(ants.gridX == x && ants.gridY == y)
      {
        action.direction = Direction.getLeftDir(direction);
      }
    }

    return true;
  }


  private AntAction chooseAction(CommData data, AntData ant)
  {
    AntAction action = new AntAction(AntActionType.STASIS);
    
    if (ant.ticksUntilNextAction > 0) return action;

    //if(birthAnt(ant,data)) return action;

    if (goHomeIfCarryingOrHurt(ant, action, data)) return action;

    if (exitNest(ant, action)) return action;

    if (pickUpWater(ant, action)) return action;

    if (goToFood(ant, action, data)) return action;

    if (goToEnemyAnt(ant, action, data)) return action;

    if (goExplore(ant, action, data)) return action;


    return action;
  }


  /**
   * The last argument is taken as the host name.
   * The default host is localhost.
   * Also supports an optional option for the teamname.
   * The default teamname is TeamNameEnum.RANDOM_WALKERS.
   * @param args Array of command-line arguments.
   */
  public static void main(String[] args)
  {
    String serverHost = "localhost";
    if (args.length > 0) serverHost = args[args.length -1];

    TeamNameEnum team = TeamNameEnum.Linh_Dustin;
    if (args.length > 1)
    { team = TeamNameEnum.getTeamByString(args[0]);
    }

    new ClientDustinLoughrin(serverHost, Constants.PORT, team);
  }

}
