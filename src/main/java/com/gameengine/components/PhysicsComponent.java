package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.math.Vector2;

public class PhysicsComponent extends Component<PhysicsComponent> {
    private Vector2 velocity = new Vector2();
    private Vector2 acceleration = new Vector2();
    private float mass = 1f;
    private float friction = 0.9f;
    private boolean useGravity = false;
    private Vector2 gravity = new Vector2(0,9.8f);

    public PhysicsComponent() {}
    public PhysicsComponent(float mass) { this.mass = mass; }

    @Override
    public void initialize() {}
    
    @Override
    public void render() {}

    public void applyForce(Vector2 force) { acceleration = acceleration.add(force.multiply(1f/mass)); }
    public void applyImpulse(Vector2 impulse) { velocity = velocity.add(impulse.multiply(1f/mass)); }

    public Vector2 getVelocity() { return new Vector2(velocity); }
    public void setVelocity(Vector2 velocity) { this.velocity = new Vector2(velocity); }
    public Vector2 getAcceleration() { return new Vector2(acceleration); }
    public void setAcceleration(Vector2 acceleration) { this.acceleration = new Vector2(acceleration); }

    public float getMass() { return mass; }
    public void setMass(float mass) { this.mass = Math.max(0.1f, mass); }

    public float getFriction() { return friction; }
    public void setFriction(float friction) { this.friction = Math.max(0, Math.min(1, friction)); }

    public boolean isUseGravity() { return useGravity; }
    public void setUseGravity(boolean useGravity) { this.useGravity = useGravity; }
    public Vector2 getGravity() { return new Vector2(gravity); }
    public void setGravity(Vector2 gravity) { this.gravity = new Vector2(gravity); }
}
