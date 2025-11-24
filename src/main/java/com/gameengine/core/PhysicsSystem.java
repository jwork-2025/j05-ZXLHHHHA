package com.gameengine.core;

import com.gameengine.components.BulletComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PhysicsSystem {
    private Scene scene;
    private ExecutorService physicsExecutor;
    private int screenWidth;
    private int screenHeight;

    public PhysicsSystem(Scene scene) {
        this(scene, 1920, 1080);
    }

    public PhysicsSystem(Scene scene, int screenWidth, int screenHeight) {
        this.scene = scene;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        this.physicsExecutor = Executors.newFixedThreadPool(threadCount);
    }

    public void update(float deltaTime) {
        List<PhysicsComponent> physicsComponents = scene.getComponents(PhysicsComponent.class);
        if (physicsComponents.isEmpty()) return;

        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        int batchSize = Math.max(1, physicsComponents.size() / threadCount + 1);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < physicsComponents.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, physicsComponents.size());

            futures.add(physicsExecutor.submit(() -> {
                for (int j = start; j < end; j++) {
                    PhysicsComponent physics = physicsComponents.get(j);
                    if (physics.isEnabled()) {
                        updatePhysics(physics, deltaTime);
                        handleBoundary(physics);
                    }
                }
            }));
        }

        for (Future<?> future : futures) {
            try { future.get(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    
    private void updatePhysics(PhysicsComponent physics, float deltaTime) {
        GameObject owner = physics.getOwner();
        if (owner == null) return;

        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) return;

        // 重力
        Vector2 acceleration = physics.getAcceleration();
        if (physics.isUseGravity()) acceleration = acceleration.add(physics.getGravity());
        physics.setAcceleration(acceleration);

        // 速度更新
        Vector2 velocity = physics.getVelocity().add(acceleration.multiply(deltaTime));
        velocity = velocity.multiply(physics.getFriction());
        physics.setVelocity(velocity);

        // 位置更新
        transform.translate(velocity.multiply(deltaTime));

        // 重置加速度
        physics.setAcceleration(new Vector2());
    }

    private void handleBoundary(PhysicsComponent physics) {
        GameObject owner = physics.getOwner();
        if (owner == null) return;

        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) return;

        Vector2 pos = transform.getPosition();
        Vector2 vel = physics.getVelocity();

        // 子弹超出边界直接销毁
        if (owner.getComponent(BulletComponent.class) != null) {
            if (pos.x < 0 || pos.x > screenWidth || pos.y < 0 || pos.y > screenHeight) {
                owner.setActive(false);
            }
            return;
        }

        // 其他物体反弹边界
        boolean velocityChanged = false;
        float velX = vel.x, velY = vel.y;
        float posX = pos.x, posY = pos.y;

        if (posX <= 0 || posX >= screenWidth - 15) { velX = -velX; velocityChanged = true; }
        if (posY <= 0 || posY >= screenHeight - 15) { velY = -velY; velocityChanged = true; }

        posX = Math.max(0, Math.min(screenWidth - 15, posX));
        posY = Math.max(0, Math.min(screenHeight - 15, posY));

        transform.setPosition(new Vector2(posX, posY));
        if (velocityChanged) physics.setVelocity(new Vector2(velX, velY));
    }

    public void cleanup() {
        if (physicsExecutor != null && !physicsExecutor.isShutdown()) {
            physicsExecutor.shutdown();
            try {
                if (!physicsExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    physicsExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                physicsExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
