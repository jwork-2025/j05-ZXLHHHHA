package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;

public class BulletComponent extends Component<BulletComponent> {
    private int damage;
    private GameObject shooter;
    private float lifetime;
    private boolean hasHit;

    public BulletComponent(int damage, GameObject shooter) {
        this.damage = damage;
        this.shooter = shooter;
        this.lifetime = 0f;
        this.hasHit = false;
    }

    @Override
    public void initialize() {}
    @Override
    public void render() {}

    public void update(float dt) {
        if (hasHit) return;
        lifetime += dt;
        if (lifetime > 3f) destroyBullet();
    }

    public void onHit(GameObject target) {
        if (hasHit || target == shooter) return;
        HealthComponent health = target.getComponent(HealthComponent.class);
        if (health != null && !health.isDead()) health.takeDamage(damage);
        hasHit = true;
        destroyBullet();
    }

    private void destroyBullet() { owner.setActive(false); }

    public boolean hasHit() { return hasHit; }
    public int getDamage() { return damage; }
    public GameObject getShooter(){return this.shooter;}
}
