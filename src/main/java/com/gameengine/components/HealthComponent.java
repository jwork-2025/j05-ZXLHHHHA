package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;

public class HealthComponent extends Component<HealthComponent> {
    private int maxHealth;
    private int currentHealth;
    private boolean showHealthBar = true;
    private float barWidth = 30f;
    private float barHeight = 6f;

    public HealthComponent(int maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
    }

    @Override
    public void initialize() {}

    @Override
    public void render() {
        if (!showHealthBar) return;
        IRenderer renderer = owner.getScene().getRenderer();
        if (renderer == null) return;

        // 获取角色位置
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) return;
        Vector2 pos = transform.getPosition();

        float healthPercent = (float)currentHealth / maxHealth;

        // 血条显示在角色上方
        
        float x = pos.x - barWidth / 2f;
        float y = pos.y - 25f; // 上方偏移

        //两个血条，方便显式血量比变化
        renderer.drawRect(x, y, barWidth, barHeight, 1, 0, 0, 1);
        renderer.drawRect(x, y, barWidth * healthPercent, barHeight, 0, 1, 0, 1);
    }
    
    public void takeDamage(int dmg) { currentHealth = Math.max(0,currentHealth-dmg); }
    public void heal(int hp) { currentHealth = Math.min(maxHealth,currentHealth+hp); }
    public boolean isDead() { return currentHealth <= 0; }

    public int getMaxHealth(){return maxHealth;}
    public int getCurrentHealth() { return currentHealth; }
    public void setShowHealthBar(boolean show) { this.showHealthBar = show; }
    public void setHealthBarSize(float width,float height) { this.barWidth=width; this.barHeight=height; }
}
