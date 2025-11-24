package com.gameengine.example;

import com.gameengine.components.HealthComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.components.ShootingComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;
import com.gameengine.core.GameObject;
// import com.gameengine.core.ParticleSystem;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import java.util.*;

public class GameScene extends Scene {
    private final GameEngine engine;
    private IRenderer renderer;
    private Random random;
    private float time;
    private GameLogic gameLogic;
    // private ParticleSystem playerParticles;
    // private List<ParticleSystem> collisionParticles;
    // private Map<GameObject, ParticleSystem> aiPlayerParticles;
    private boolean waitingReturn;
    private float waitInputTimer;
    private float freezeTimer;
    private final float inputCooldown = 0.25f;
    private final float freezeDelay = 0.20f;

    public GameScene(GameEngine engine) {
        super("GameScene");
        this.engine = engine;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.random = new Random();
        this.time = 0;
        this.gameLogic = new GameLogic(this);
        this.gameLogic.setGameEngine(engine);
        this.waitingReturn = false;
        this.waitInputTimer = 0f;
        this.freezeTimer = 0f;

        createPlayer();
        //createAIPlayer();
        //createAIPlayers();
        createDecorations();

        // collisionParticles = new ArrayList<>();
        // aiPlayerParticles = new HashMap<>();

        // playerParticles = new ParticleSystem(renderer, new Vector2(renderer.getWidth() / 2.0f, renderer.getHeight() / 2.0f));
        // playerParticles.setActive(true);
        
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        time += deltaTime;

        gameLogic.handlePlayerInput(deltaTime);
        gameLogic.handleAIPlayerMovement(deltaTime);
        gameLogic.handleAIPlayerAvoidance(deltaTime);

        boolean wasGameOver = gameLogic.isGameOver();
        gameLogic.checkCollisions();
        gameLogic.checkBulletCollisions();
        gameLogic.cleanupDeadObjects();
        /* 
        if (gameLogic.isGameOver() && !wasGameOver) {
            GameObject player = gameLogic.getUserPlayer();
            if (player != null) {
                TransformComponent transform = player.getComponent(TransformComponent.class);
                if (transform != null) {
                    
                    ParticleSystem.Config cfg = new ParticleSystem.Config();
                    cfg.initialCount = 0;
                    cfg.spawnRate = 9999f;
                    cfg.opacityMultiplier = 1.0f;
                    cfg.minRenderSize = 3.0f;
                    cfg.burstSpeedMin = 250f;
                    cfg.burstSpeedMax = 520f;
                    cfg.burstLifeMin = 0.5f;
                    cfg.burstLifeMax = 1.2f;
                    cfg.burstSizeMin = 18f;
                    cfg.burstSizeMax = 42f;
                    cfg.burstR = 1.0f;
                    cfg.burstGMin = 0.0f;
                    cfg.burstGMax = 0.05f;
                    cfg.burstB = 0.0f;
                    ParticleSystem explosion = new ParticleSystem(renderer, transform.getPosition(), cfg);
                    explosion.burst(180);
                    collisionParticles.add(explosion);
                    waitingReturn = true;
                    waitInputTimer = 0f;
                    freezeTimer = 0f;
                    
                }
            }
        }
        */

        // updateParticles(deltaTime);

        if (gameLogic.isGameOver() && !wasGameOver) {
            waitingReturn = true;  // 游戏结束后启用等待状态
            waitInputTimer = 0f;
            freezeTimer = 0f;
        }

        
        if (waitingReturn) {
            waitInputTimer += deltaTime;
            freezeTimer += deltaTime;
        }

        if (waitingReturn && waitInputTimer >= inputCooldown &&(engine.getInputManager().isAnyKeyJustPressed() || engine.getInputManager().isMouseButtonJustPressed(0))) {

            MenuScene menu = new MenuScene(engine, "MainMenu");
            engine.setScene(menu);
            return;
        }



        
        
        // 只有在游戏未结束时生成AI
        if (!gameLogic.isGameOver() && time >= 1.0f) {
            createAIPlayer();
            time = 0;
        }
        
    }

    /*
    private void updateParticles(float deltaTime) {
        boolean freeze = waitingReturn && freezeTimer >= freezeDelay;

        if (playerParticles != null && !freeze) {
            GameObject player = gameLogic.getUserPlayer();
            if (player != null) {
                TransformComponent transform = player.getComponent(TransformComponent.class);
                if (transform != null) {
                    Vector2 playerPos = transform.getPosition();
                    playerParticles.setPosition(playerPos);
                }
            }
            playerParticles.update(deltaTime);
        }

        List<GameObject> aiPlayers = gameLogic.getAIPlayers();
        if (!freeze) {
            for (GameObject aiPlayer : aiPlayers) {
                if (aiPlayer != null && aiPlayer.isActive()) {
                    ParticleSystem particles = aiPlayerParticles.get(aiPlayer);
                    if (particles == null) {
                        TransformComponent transform = aiPlayer.getComponent(TransformComponent.class);
                        if (transform != null) {
                            particles = new ParticleSystem(renderer, transform.getPosition(), ParticleSystem.Config.light());
                            particles.setActive(true);
                            aiPlayerParticles.put(aiPlayer, particles);
                        }
                    }
                    if (particles != null) {
                        TransformComponent transform = aiPlayer.getComponent(TransformComponent.class);
                        if (transform != null) {
                            particles.setPosition(transform.getPosition());
                        }
                        particles.update(deltaTime);
                    }
                }
            }
        }

        List<GameObject> toRemove = new ArrayList<>();
        for (Map.Entry<GameObject, ParticleSystem> entry : aiPlayerParticles.entrySet()) {
            if (!entry.getKey().isActive() || !aiPlayers.contains(entry.getKey())) {
                toRemove.add(entry.getKey());
            }
        }
        for (GameObject removed : toRemove) {
            aiPlayerParticles.remove(removed);
        }

        for (int i = collisionParticles.size() - 1; i >= 0; i--) {
            ParticleSystem ps = collisionParticles.get(i);
            if (ps != null) {
                if (!freeze) {
                    ps.update(deltaTime);
                }
            }
        }
    }
    */

    @Override
    public void render() {
        renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.1f, 0.1f, 0.2f, 1.0f);

        super.render();

        // renderParticles();

        if (gameLogic.isGameOver()) {
            float cx = renderer.getWidth() / 2.0f;
            float cy = renderer.getHeight() / 2.0f;

            // 半透明全屏背景
            renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0f, 0f, 0f, 0.35f);

            // 背景方块更大，让文字突出
            renderer.drawRect(cx - 300, cy - 100, 600, 200, 0f, 0f, 0f, 0.7f);

            // 红色文字，模拟大号粗体
            String gameOverText = "GAME OVER";
            // 扩大偏移量，让文字更大
            for (int dx = -6; dx <= 6; dx++) {
                for (int dy = -6; dy <= 6; dy++) {
                    renderer.drawText(cx - 150 + dx, cy - 30 + dy, gameOverText, 1f, 0f, 0f, 1f);
                }
            }

            // 提示文字
            renderer.drawText(cx - 220, cy + 90, "PRESS ANY KEY TO RETURN", 0.8f, 0.8f, 0.8f, 1f);
        }


    }

    /*
    private void renderParticles() {
        if (playerParticles != null) {
            int count = playerParticles.getParticleCount();
            if (count > 0) {
                playerParticles.render();
            }
        }

        for (ParticleSystem ps : aiPlayerParticles.values()) {
            if (ps != null && ps.getParticleCount() > 0) {
                ps.render();
            }
        }

        for (ParticleSystem ps : collisionParticles) {
            if (ps != null && ps.getParticleCount() > 0) {
                ps.render();
            }
        }
    }
    */
    
    private void createPlayer() {
        GameObject player = new GameObject("Player") {
            private Vector2 basePosition;

            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
                updateBodyParts();
            }

            @Override
            public void render() {
                renderBodyParts();
                // 渲染组件（血条、射击）
                renderComponents();
            }

            private void updateBodyParts() {
                TransformComponent transform = getComponent(TransformComponent.class);
                if (transform != null) {
                    basePosition = transform.getPosition();
                }
            }

            private void renderBodyParts() {
                if (basePosition == null) return;

                // 身体和装饰
                renderer.drawRect(basePosition.x - 8, basePosition.y - 10, 16, 20, 1f, 0f, 0f, 1f);
                renderer.drawRect(basePosition.x - 6, basePosition.y - 22, 12, 12, 1f, 0.5f, 0f, 1f);
                renderer.drawRect(basePosition.x - 13, basePosition.y - 5, 6, 12, 1f, 0.8f, 0f, 1f);
                renderer.drawRect(basePosition.x + 7, basePosition.y - 5, 6, 12, 0f, 1f, 0f, 1f);
            }
        };

        //设定场景
        player.setScene(this);

        // Transform
        player.addComponent(new TransformComponent(new Vector2(renderer.getWidth() / 2f, renderer.getHeight() / 2f)));

        // Physics
        PhysicsComponent physics = player.addComponent(new PhysicsComponent(1f));
        physics.setFriction(0.95f);

        // Health
        HealthComponent health = player.addComponent(new HealthComponent(100));
        health.setShowHealthBar(true);
        health.setHealthBarSize(30, 6);

        // Shooting（手动射击）
        ShootingComponent shooting = player.addComponent(new ShootingComponent(1f, 200f, 15, false));

        addGameObject(player);
    }

    private void createAIPlayers() {
        for (int i = 0; i < 30; i++) {
            createAIPlayer();
        }
    }

    private static int aiPlayerCount = 0;
    private void createAIPlayer() {
        GameObject aiPlayer = new GameObject("AIPlayer"+aiPlayerCount++) {
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
            }

            @Override
            public void render() {
                renderComponents();
            }
        };

        Vector2 position;
        do {
            position = new Vector2(random.nextFloat() * renderer.getWidth(), random.nextFloat() * renderer.getHeight());
        } while (position.distance(new Vector2(renderer.getWidth() / 2f, renderer.getHeight() / 2f)) < 100);

        aiPlayer.setScene(this);

        // Transform
        aiPlayer.addComponent(new TransformComponent(position));

        // Render
        RenderComponent rc = aiPlayer.addComponent(new RenderComponent(
                RenderComponent.RenderType.RECTANGLE,
                new Vector2(20, 20),
                new RenderComponent.Color(0f, 0.8f, 1f, 1f)
        ));
        rc.setRenderer(renderer);

        // Physics
        PhysicsComponent physics = aiPlayer.addComponent(new PhysicsComponent(0.5f));
        physics.setVelocity(new Vector2((random.nextFloat() - 0.5f) * 150, (random.nextFloat() - 0.5f) * 150));
        physics.setFriction(0.98f);

        // Health
        HealthComponent health = aiPlayer.addComponent(new HealthComponent(50));
        health.setShowHealthBar(true);
        health.setHealthBarSize(30, 6);

        // Shooting（自动射击玩家）
        ShootingComponent shooting = aiPlayer.addComponent(new ShootingComponent(5f, 100f, 10, true));

        addGameObject(aiPlayer);
    }


    
    private void createDecorations() {
        for (int i = 0; i < 5; i++) {
            createDecoration();
        }
    }

    private static int decorationCount = 0;
    private void createDecoration() {
        GameObject decoration = new GameObject("Decoration"+decorationCount++) {
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
            }

            @Override
            public void render() {
                renderComponents();
            }
        };

        Vector2 position = new Vector2(
            random.nextFloat() * renderer.getWidth(),
            random.nextFloat() * renderer.getHeight()
        );

        //
        decoration.setScene(this);
        decoration.addComponent(new TransformComponent(position));

        RenderComponent render = decoration.addComponent(new RenderComponent(
            RenderComponent.RenderType.CIRCLE,
            new Vector2(5, 5),
            new RenderComponent.Color(0.5f, 0.5f, 1.0f, 0.8f)
        ));
        render.setRenderer(renderer);

        addGameObject(decoration);
    }

    @Override
    public void clear() {
        if (gameLogic != null) {
            gameLogic.cleanup();
        }
        
        /*
        if (playerParticles != null) {
            playerParticles.clear();
        }
        if (collisionParticles != null) {
            for (ParticleSystem ps : collisionParticles) {
                if (ps != null) ps.clear();
            }
            collisionParticles.clear();
        }
        */
        
        super.clear();
    }

    public IRenderer getRenderer() {return this.renderer;}
}
