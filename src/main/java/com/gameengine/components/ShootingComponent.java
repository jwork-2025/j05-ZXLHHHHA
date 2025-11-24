package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

/**
 * 射击组件 - 支持自动和手动射击
 */
public class ShootingComponent extends Component<ShootingComponent> {
    private float fireRate;      // 发射速率（秒/发）
    private float fireTimer;     // 发射计时器
    private float bulletSpeed;   // 子弹速度
    private int bulletDamage;    // 子弹伤害
    private boolean autoShoot;   // 自动射击模式

    public ShootingComponent() {
        this(0.3f, 500.0f, 10, false); // 默认手动射击
    }

    public ShootingComponent(float fireRate, float bulletSpeed, int bulletDamage, boolean autoShoot) {
        this.fireRate = fireRate;
        this.fireTimer = 0;
        this.bulletSpeed = bulletSpeed;
        this.bulletDamage = bulletDamage;
        this.autoShoot = autoShoot;
    }

    @Override
    public void initialize() {
        System.out.println(getOwner().getName() + " 射击组件初始化 - 模式: " + (autoShoot ? "自动" : "手动"));
    }

    @Override
    public void render() {
        
    }
    @Override
    public void update(float deltaTime) {
        if (!enabled) return;
        fireTimer += deltaTime;

        if (autoShoot) {
            updateAutoShoot();
        } else {
            updateManualShoot();
        }
    }

    private void updateAutoShoot() {
        if (fireTimer >= fireRate) {
            GameObject target = findAutoShootTarget();
            if (target != null) {
                shootTowards(target);
                fireTimer = 0;
            }
        }
    }

    private void updateManualShoot() {
        InputManager input = InputManager.getInstance();
        if (input.isMouseButtonPressed(0) && fireTimer >= fireRate) {
            shoot();
            fireTimer = 0;
        }
    }

    private GameObject findAutoShootTarget() {
        Scene scene = getOwner().getScene();
        if (scene == null) return null;

        if (getOwner().getName().equals("Player")) {
            return scene.getGameObjects().stream()
                .filter(obj -> obj.getName().startsWith("Enemy") && obj.isActive())
                .findFirst()
                .orElse(null);
        } else {
            return scene.getGameObjects().stream()
                .filter(obj -> obj.getName().equals("Player") && obj.isActive())
                .findFirst()
                .orElse(null);
        }
    }

    public void shoot() {
        InputManager input = InputManager.getInstance();
        shootTowards(input.getMousePosition());
    }

    public void shootTowards(Vector2 targetPosition) {
        TransformComponent transform = getOwner().getComponent(TransformComponent.class);
        if (transform == null) return;

        Vector2 direction = targetPosition.subtract(transform.getPosition()).normalize();
        createBullet(transform.getPosition(), direction);
    }

    public void shootTowards(GameObject target) {
        if (target == null) return;
        TransformComponent targetTransform = target.getComponent(TransformComponent.class);
        if (targetTransform != null) shootTowards(targetTransform.getPosition());
    }

    private  static int BulletCount = 0;
    private void createBullet(Vector2 position, Vector2 direction) {
        GameObject bullet = new GameObject("Bullet"+BulletCount);
        BulletCount++;
        bullet.addComponent(new TransformComponent(position.add(direction.multiply(20))));
        RenderComponent render = new RenderComponent(RenderComponent.RenderType.CIRCLE, new Vector2(8,8),
                getOwner().getName().equals("Player")
                        ? new RenderComponent.Color(0,0.5f,1f,1f)
                        : new RenderComponent.Color(1f,0.3f,0.3f,1f));
        render.setRenderer(getOwner().getScene().getRenderer());
        bullet.addComponent(render);

        PhysicsComponent physics = new PhysicsComponent(0.1f);
        physics.setVelocity(direction.multiply(bulletSpeed));
        physics.setFriction(1.0f);
        bullet.addComponent(physics);

        bullet.addComponent(new BulletComponent(bulletDamage, getOwner()));

        Scene scene = getOwner().getScene();
        if (scene != null) scene.addGameObject(bullet);
    }

    // Getters & Setters
    public float getFireRate() { return fireRate; }
    public void setFireRate(float fireRate) { this.fireRate = Math.max(0.1f, fireRate); }
    public float getBulletSpeed() { return bulletSpeed; }
    public void setBulletSpeed(float bulletSpeed) { this.bulletSpeed = Math.max(0, bulletSpeed); }
    public int getBulletDamage() { return bulletDamage; }
    public void setBulletDamage(int bulletDamage) { this.bulletDamage = Math.max(1, bulletDamage); }
    public boolean isAutoShoot() { return autoShoot; }
    public void setAutoShoot(boolean autoShoot) { this.autoShoot = autoShoot; }
    public float getFireTimer() { return fireTimer; }
    public boolean canShoot() { return fireTimer >= fireRate; }
}
