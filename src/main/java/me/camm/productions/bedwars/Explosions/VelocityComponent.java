package me.camm.productions.bedwars.Explosions;


import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;

import java.util.List;

public class VelocityComponent {
    private final EntityExplodeEvent event;
    private boolean isFireball;

    public VelocityComponent(EntityExplodeEvent event) {
        this.event = event;
        if (event.getEntityType() == EntityType.SMALL_FIREBALL || event.getEntityType() == EntityType.FIREBALL)
            this.isFireball = true;

    }

    public void doCalculation()  //unfinished
    {
        //System.out.println("Invoked calculation method");
        Entity exploded = this.event.getEntity();


        List<Entity> nearBy = exploded.getNearbyEntities(exploded.getLocation().getX(), exploded.getLocation().getY(), exploded.getLocation().getZ());

        for (int slot = 0; slot < nearBy.size(); slot++) {
            if (validType(nearBy.get(slot))) {
                if ((exploded.getLocation().distance(nearBy.get(slot).getLocation()) <= 5) && isFireball)  //if the distance is <= 6 and is fireball
                    constructFireVector(exploded.getLocation(), nearBy.get(slot).getLocation(), nearBy.get(slot));

                else if ((exploded.getLocation().distance(nearBy.get(slot).getLocation()) <= 8) && (!isFireball))
                    constructTNTVector(exploded.getLocation(), nearBy.get(slot).getLocation(), nearBy.get(slot));

            } //if is valid
        }//for nearby
    }//method


    public boolean validType(Entity type) {
        return type.getType() != EntityType.FIREBALL
                && type.getType() != EntityType.ARMOR_STAND && type.getType() != EntityType.ENDER_DRAGON &&
                type.getType() != EntityType.PRIMED_TNT && type.getType() != EntityType.DROPPED_ITEM;
    }

    public void constructFireVector(Location origin, Location destination, Entity target)/////////////////tnt
    {
        //System.out.println("Using fireball velocity");
        double distanceX, distanceY, distanceZ;
        boolean xNegative = false, yNegative = false, zNegative = false;
        boolean horizontalValid = true;

        double xComponent, yComponent, zComponent, horizontalComponent;
        boolean deconstruct = true;

        double horizontalAngle, verticalAngle;
        double horizontalDistance, totalDistance;

        //getting the distances
        distanceX = constructDistance(origin.getX(), destination.getX());
        distanceY = constructDistance(origin.getY(), destination.getY() + 0.5);  //Account for hitbox [0.5]
        distanceZ = constructDistance(origin.getZ(), destination.getZ());


        //if any are negative
        if (distanceX < 0)  //Adjacent [horizontal]
        {
            distanceX = Math.abs(distanceX);
            xNegative = true;
        }

        if (distanceY < 0) {
            distanceY = Math.abs(distanceY);  //tangent
            yNegative = true;
        }

        if (distanceZ < 0) //Opposite [horizontal]
        {
            distanceZ = Math.abs(distanceZ);
            zNegative = true;
        }

        //horizontal
        if (distanceX != 0 && distanceZ != 0)  //if both are not 0
        {
            horizontalAngle = Math.toDegrees(Math.atan(distanceZ / distanceX));
            horizontalDistance = Math.sqrt((distanceX * distanceX) + (distanceZ * distanceZ));
        } else  //if there is at least zero
        {
            if (distanceX == 0 && distanceZ == 0) {
                horizontalDistance = 0;  //the entity should travel directly up
                horizontalValid = false;  //so we know not to use trig to find the ratios
                horizontalAngle = 0;
            } else  //if the 2 distances are not 0, but 1 is [travel in 1 direction,but not the other]
            {
                if (distanceX == 0) //Adjacent [horizontal]
                {
                    horizontalAngle = 90;  // b/c cos90 = 0, but sin90 = 1
                    horizontalDistance = distanceZ;
                } else  //if distanceX is not 0, then distanceZ is
                {
                    horizontalDistance = distanceX;
                    horizontalAngle = 0;  // b/c cos0 = 1, but sin0  = 0
                }

            }
        }

        //vertical
        if (distanceY != 0 && horizontalDistance != 0)  //if the vertical and horizontal distances are not 0
        {
            verticalAngle = Math.toDegrees(Math.atan(distanceY / horizontalDistance));

            totalDistance = Math.sqrt((distanceY * distanceY) + (horizontalDistance * horizontalDistance));  //pytha. theorem
        } else  //at least 1 is 0
        {
            if (distanceY == 0 && horizontalDistance == 0)  //if both are 0
            {
                //there is no knockback in this case.
                totalDistance = 0;
                verticalAngle = 0;
                horizontalAngle = 0;
                deconstruct = false;  //we should not deconstruct the vector. It is fine as is.

            } else  //just 1 is 0
            {
                if (distanceY == 0)  //if the y component is 0     --> hsin90 = 0
                {
                    // System.out.println("Y distance is 0");
                    verticalAngle = 0;
                    totalDistance = horizontalDistance;   //there is no vertical component
                } else  //if the y distance not 0, then the h distance is
                {
                    //  System.out.println("Case 2: Y distance not 0");
                    verticalAngle = 90; //hcos0 = 1
                    totalDistance = distanceY;
                }
            }
        }


        if (deconstruct)  //if we should deconstruct the vector [deconstruct is true]
        {
            /////////////////////////////////////////////////
            //FORMULA
            ////////////////////////////////////////////////
            totalDistance = 1.65 / ((0.01 * Math.pow(totalDistance, (0.5 * (Math.pow(totalDistance, 0.5)) + 2))) + 1);


            yComponent = ((totalDistance * (Math.sin(Math.toRadians(verticalAngle)))));

            double placeHold;

            if (horizontalValid)  //if the velocity is not directly upwards
            {
                horizontalComponent = totalDistance * (Math.cos(Math.toRadians(verticalAngle)));
                xComponent = (horizontalComponent * (Math.cos(Math.toRadians(horizontalAngle))));
                zComponent = (horizontalComponent * (Math.sin(Math.toRadians(horizontalAngle))));

                //Alter the velocities, make vertical more substantial
                //Magnitude shift.
                placeHold = xComponent;  //placehold

                xComponent *= 0.65;
                yComponent = yComponent + (placeHold - xComponent);  //magnitude [Take the recipricol]

                placeHold = zComponent;
                zComponent *= 0.65;
                yComponent = yComponent + (placeHold - zComponent);  //magnitude

                if (yNegative)
                    yComponent *= -1;

                if (xNegative)
                    xComponent *= -1;

                if (zNegative)
                    zComponent *= -1;


                impartVelocity(xComponent, yComponent, zComponent, target);
            } else {


                if (yNegative)
                    yComponent *= -1;


                impartVelocity(0, yComponent, 0, target);
            }
        } else {

            impartVelocity(0, 0, 0, target);

        }
    }


    public void constructTNTVector(Location origin, Location destination, Entity target) /////////////////tnt
    {
        //System.out.println("Using tnt velocity formula");
        double distanceX, distanceY, distanceZ;
        boolean xNegative = false, yNegative = false, zNegative = false;
        boolean horizontalValid = true;

        double xComponent, yComponent, zComponent, horizontalComponent;
        boolean deconstruct = true;

        double horizontalAngle, verticalAngle;
        double horizontalDistance, totalDistance;

        //getting the distances
        distanceX = constructDistance(origin.getX(), destination.getX());
        distanceY = constructDistance(origin.getY(), destination.getY() + 0.5);  //Account for hitbox [0.5]
        distanceZ = constructDistance(origin.getZ(), destination.getZ());


        //if any are negative
        if (distanceX < 0)  //Adjacent [horizontal]
        {
            distanceX = Math.abs(distanceX);
            xNegative = true;
        }

        if (distanceY < 0) {
            distanceY = Math.abs(distanceY);  //tangent
            yNegative = true;
        }

        if (distanceZ < 0) //Opposite [horizontal]
        {
            distanceZ = Math.abs(distanceZ);
            zNegative = true;
        }

        //horizontal
        if (distanceX != 0 && distanceZ != 0)  //if both are not 0
        {
            horizontalAngle = Math.toDegrees(Math.atan(distanceZ / distanceX));
            horizontalDistance = Math.sqrt((distanceX * distanceX) + (distanceZ * distanceZ));
        } else  //if there is at least zero
        {
            if (distanceX == 0 && distanceZ == 0) {
                horizontalDistance = 0;  //the entity should travel directly up
                horizontalValid = false;  //so we know not to use trig to find the ratios
                horizontalAngle = 0;
            } else  //if the 2 distances are not 0, but 1 is [travel in 1 direction,but not the other]
            {
                if (distanceX == 0) //Adjacent [horizontal]
                {
                    horizontalAngle = 90;  // b/c cos90 = 0, but sin90 = 1
                    horizontalDistance = distanceZ;
                } else  //if distanceX is not 0, then distanceZ is
                {
                    horizontalDistance = distanceX;
                    horizontalAngle = 0;  // b/c cos0 = 1, but sin0  = 0
                }

            }
        }

        //vertical
        if (distanceY != 0 && horizontalDistance != 0)  //if the vertical and horizontal distances are not 0
        {
            verticalAngle = Math.toDegrees(Math.atan(distanceY / horizontalDistance));
            totalDistance = Math.sqrt((distanceY * distanceY) + (horizontalDistance * horizontalDistance));  //pytha. theorem
        } else  //at least 1 is 0
        {
            if (distanceY == 0 && horizontalDistance == 0)  //if both are 0
            {
                //there is no knockback in this case.
                totalDistance = 0;
                verticalAngle = 0;
                horizontalAngle = 0;
                deconstruct = false;  //we should not deconstruct the vector. It is fine as is.

            } else  //just 1 is 0
            {
                if (distanceY == 0)  //if the y component is 0     --> hsin90 = 0
                {
                    verticalAngle = 90;
                    totalDistance = horizontalDistance;   //there is no vertical component
                } else  //if the y distance not 0, then the h distance is
                {
                    verticalAngle = 0; //hcos0 = 1
                    totalDistance = distanceY;
                }
            }
        }

        if (deconstruct)  //if we should deconstruct the vector [deconstruct is true]
        {
            /////////////////////////////////////////////////
            //FORMULA
            ////////////////////////////////////////////////
            totalDistance = 1.65 / ((0.04 * Math.pow(totalDistance, (0.4 * (Math.pow(totalDistance, 0.5)) + 2))) + 1);

            // endX = y*(Math.cos(Math.toRadians(angle)));
            yComponent = ((totalDistance * (Math.sin(Math.toRadians(verticalAngle)))));

            double placeHold;

            if (horizontalValid)  //if the velocity is not directly upwards
            {
                horizontalComponent = totalDistance * (Math.cos(Math.toRadians(verticalAngle)));
                xComponent = (horizontalComponent * (Math.cos(Math.toRadians(horizontalAngle))));
                zComponent = (horizontalComponent * (Math.sin(Math.toRadians(horizontalAngle))));

                //Alter the velocities, make vertical more substantial
                //Magnitude shift.
                placeHold = xComponent;  //placehold

                xComponent *= 0.65;
                yComponent = yComponent + (placeHold - xComponent);  //magnitude [Take the recipricol]
                xComponent = placeHold * 0.8;


                placeHold = zComponent;
                zComponent *= 0.65;
                yComponent = yComponent + (placeHold - zComponent);  //magnitude
                zComponent = placeHold * 0.8;

                if (yNegative)
                    yComponent *= -1;

                if (xNegative)
                    xComponent *= -1;

                if (zNegative)
                    zComponent *= -1;

                impartVelocity(xComponent, yComponent, zComponent, target);
            } else {
                if (yNegative)
                    yComponent *= -1;

                impartVelocity(0, yComponent, 0, target);
            }
        } else {
            impartVelocity(0, 0, 0, target);
        }

    }/////////////////tnt


    private void impartVelocity(double xComponent, double yComponent, double zComponent, Entity targeted)
    {
        Vector velocity = new Vector(xComponent,yComponent,zComponent);
        targeted.setVelocity(velocity);
    }


    public double constructDistance(double origin, double destination)
    {
        return destination - origin;
    }

}
