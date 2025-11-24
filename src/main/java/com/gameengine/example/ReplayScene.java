package com.gameengine.example;

import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.IRenderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import java.io.File;
import java.util.*;

public class ReplayScene extends Scene {
    private final GameEngine engine;
    private String recordingPath;
    private IRenderer renderer;
    private InputManager input;
    private float time;

    private static class Keyframe {
        static class EntityInfo {
            Vector2 pos;
            String rt; // RECTANGLE/CIRCLE/LINE/CUSTOM/null
            float w, h;
            float r = 0.9f, g = 0.9f, b = 0.2f, a = 1.0f; // 默认颜色
            String id;
            float hp = 100f;  
            float maxHp = 100f; 
        }
        double t;
        List<EntityInfo> entities = new ArrayList<>();
    }

    private final List<Keyframe> keyframes = new ArrayList<>();
    private final Map<String, GameObject> objectMap = new HashMap<>();

    public ReplayScene(GameEngine engine, String path) {
        super("Replay");
        this.engine = engine;
        this.recordingPath = path;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.input = engine.getInputManager();
        this.time = 0f;
        keyframes.clear();
        objectMap.clear();
        if (recordingPath != null) {
            loadRecording(recordingPath);
        } else {
            recordingFiles = null;
            selectedIndex = 0;
        }
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        if (input.isKeyJustPressed(27) || input.isKeyJustPressed(256) || input.isKeyJustPressed(8)) { // ESC/BACK
            engine.setScene(new MenuScene(engine, "MainMenu"));
            return;
        }

        if (recordingPath == null) {
            handleFileSelection();
            return;
        }

        if (keyframes.isEmpty()) return;

        time += deltaTime;
        double lastT = keyframes.get(keyframes.size() - 1).t;
        if (time > lastT) 
        {
            time = (float) lastT;
        }
            

        // 查找当前帧对应的 keyframe 区间 a->b
        Keyframe a = keyframes.get(0);
        Keyframe b = keyframes.get(keyframes.size() - 1);
        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe k1 = keyframes.get(i);
            Keyframe k2 = keyframes.get(i + 1);
            if (time >= k1.t && time <= k2.t) { a = k1; b = k2; break; }
        }

        double span = Math.max(1e-6, b.t - a.t);
        double u = (time - a.t) / span;

        updateDynamicObjects(a, b, (float) u);
    }

    @Override
    public void render() {
        // 背景
        renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.06f, 0.06f, 0.08f, 1.0f);

        if (recordingPath == null) {
            renderFileList();
            return;
        }

        super.render();

        for (Map.Entry<String, GameObject> entry : objectMap.entrySet()) {
            GameObject obj = entry.getValue();
            Keyframe.EntityInfo ei = findEntityInfo(obj.getName());
            if (ei == null) continue;

            // 只给 Player 和 AIPlayer 显示血条
            if (!ei.id.equalsIgnoreCase("Player") && !ei.id.startsWith("AIPlayer")) continue;

            // 血条位置在对象上方
            float barWidth = 30f;
            float barHeight = 6f;
            TransformComponent tc = obj.getComponent(TransformComponent.class);
            Vector2 pos = (tc != null ? tc.getPosition() : ei.pos);
            float x,y;
            if(ei.id.equalsIgnoreCase("Player"))
            {
                x = pos.x - barWidth / 2f;
                y= pos.y - 30;
            }
            else{
                x = pos.x - barWidth / 2f;
                y = pos.y - ei.h / 2f - 10f;
            }
            

            // 红色背景
            renderer.drawRect(x, y, barWidth, barHeight, 0.8f, 0.0f, 0.0f, 1.0f);

            // 绿色前景显示血量
            float hpPercent = Math.max(0f, Math.min(1f, ei.hp / ei.maxHp));
            renderer.drawRect(x, y, barWidth * hpPercent, barHeight, 0.0f, 1.0f, 0.0f, 1.0f);
        }
        
        // 提示文字
        String hint = "REPLAY: ESC to return";
        float w = hint.length() * 12.0f;
        renderer.drawText(renderer.getWidth() / 2.0f - w / 2.0f, 30, hint, 0.8f, 0.8f, 0.8f, 1.0f);
    }


    private void loadRecording(String path) {
        keyframes.clear();
        com.gameengine.recording.RecordingStorage storage = new com.gameengine.recording.FileRecordingStorage();
        try {
            for (String line : storage.readLines(path)) {
                if (!line.contains("\"type\":\"keyframe\"")) continue;

                Keyframe kf = new Keyframe();
                kf.t = com.gameengine.recording.RecordingJson.parseDouble(
                        com.gameengine.recording.RecordingJson.field(line, "t")
                );

                String entitiesArr = com.gameengine.recording.RecordingJson.field(line, "entities");
                if (entitiesArr != null && entitiesArr.startsWith("[") && entitiesArr.endsWith("]")) {
                    // 去掉首尾 []
                    entitiesArr = entitiesArr.substring(1, entitiesArr.length() - 1).trim();
                    String[] parts = com.gameengine.recording.RecordingJson.splitTopLevel(entitiesArr);

                    for (String p : parts) {
                        Keyframe.EntityInfo ei = new Keyframe.EntityInfo();
                        ei.id = com.gameengine.recording.RecordingJson.stripQuotes(
                                com.gameengine.recording.RecordingJson.field(p, "id")
                        );

                        double x = com.gameengine.recording.RecordingJson.parseDouble(
                                com.gameengine.recording.RecordingJson.field(p, "x")
                        );
                        double y = com.gameengine.recording.RecordingJson.parseDouble(
                                com.gameengine.recording.RecordingJson.field(p, "y")
                        );
                        ei.pos = new Vector2((float) x, (float) y);

                        ei.rt = com.gameengine.recording.RecordingJson.stripQuotes(
                                com.gameengine.recording.RecordingJson.field(p, "rt")
                        );

                        ei.w = (float) com.gameengine.recording.RecordingJson.parseDouble(
                                com.gameengine.recording.RecordingJson.field(p, "w")
                        );
                        ei.h = (float) com.gameengine.recording.RecordingJson.parseDouble(
                                com.gameengine.recording.RecordingJson.field(p, "h")
                        );

                        // ===== 解析 color 字段 =====
                        String colorField = com.gameengine.recording.RecordingJson.field(p, "color");
                        if (colorField != null && colorField.startsWith("[") && colorField.endsWith("]")) {
                            String content = colorField.substring(1, colorField.length() - 1).trim();
                            String[] cs = content.split(",");
                            try {
                                if (cs.length >= 1) ei.r = Float.parseFloat(cs[0].trim());
                                if (cs.length >= 2) ei.g = Float.parseFloat(cs[1].trim());
                                if (cs.length >= 3) ei.b = Float.parseFloat(cs[2].trim());
                                if (cs.length >= 4) ei.a = Float.parseFloat(cs[3].trim());
                            } catch (Exception ignored) {}
                        }

                        ei.hp = (float) com.gameengine.recording.RecordingJson.parseDouble(
                                com.gameengine.recording.RecordingJson.field(p, "hp"));
                        ei.maxHp = (float) com.gameengine.recording.RecordingJson.parseDouble(
                                com.gameengine.recording.RecordingJson.field(p, "maxHp"));

                        kf.entities.add(ei);
                    }
                }

                keyframes.add(kf);
            }
        } catch (Exception ignored) {}

        keyframes.sort((k1, k2) -> Double.compare(k1.t, k2.t));
    }


    private Keyframe.EntityInfo findEntityInfo(String id) {
        if (keyframes.isEmpty()) return null;

        Keyframe kf = keyframes.get(keyframes.size() - 1); // 默认最后一帧
        for (int i = 0; i < keyframes.size() - 1; i++) {
            if (time >= keyframes.get(i).t && time <= keyframes.get(i+1).t) {
                kf = keyframes.get(i+1); // 取靠后的帧
                break;
            }
        }

        for (Keyframe.EntityInfo ei : kf.entities) {
            if (ei.id.equals(id)) return ei;
        }
        return null;
    }


    private void updateDynamicObjects(Keyframe a, Keyframe b, float u) {
        // 将 b 的实体按 id 做 map
        Map<String, Keyframe.EntityInfo> bMap = new HashMap<>();
        for (Keyframe.EntityInfo ei : b.entities) bMap.put(ei.id, ei);

        // 更新 a.entities 对应的对象
        for (Keyframe.EntityInfo eiA : a.entities) {
            Keyframe.EntityInfo eiB = bMap.get(eiA.id);
            GameObject obj = objectMap.get(eiA.id);

            if (obj == null) {
                obj = buildObjectFromEntity(eiA);
                addGameObject(obj);
                objectMap.put(eiA.id, obj);
            }

            Vector2 posA = eiA.pos;
            Vector2 posB = (eiB != null ? eiB.pos : eiA.pos);
            float x = (1 - u) * posA.x + u * posB.x;
            float y = (1 - u) * posA.y + u * posB.y;

            TransformComponent tc = obj.getComponent(TransformComponent.class);
            if (tc != null) tc.setPosition(new Vector2(x, y));
        }

        // b.entities 中新出现的对象
        for (Keyframe.EntityInfo eiB : b.entities) {
            if (!objectMap.containsKey(eiB.id)) {
                GameObject obj = buildObjectFromEntity(eiB);
                addGameObject(obj);
                objectMap.put(eiB.id, obj);
            }
        }

        // 移除 a 和 b 中不再出现的对象（死亡）
        // 移除不再出现的对象
        Set<String> aliveIds = new HashSet<>();
        for (Keyframe.EntityInfo ei : b.entities) aliveIds.add(ei.id);

        // 保留 Player
        aliveIds.add("Player"); 

        Iterator<Map.Entry<String, GameObject>> it = objectMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, GameObject> entry = it.next();
            if (!aliveIds.contains(entry.getKey())) {
                entry.getValue().setActive(false);
                it.remove();
            }
        }

    }

    private GameObject buildObjectFromEntity(Keyframe.EntityInfo ei) {
        GameObject obj;

        if ("Player".equalsIgnoreCase(ei.id)) {
            // 唯一玩家
            obj = com.gameengine.example.EntityFactory.createPlayerVisual(renderer);
        } 
        else if (ei.id.startsWith("AIPlayer")) {
            // AI 玩家，带编号
            obj = com.gameengine.example.EntityFactory.createAIVisual(renderer, ei.w, ei.h, ei.r, ei.g, ei.b, ei.a);
        } 
        else if (ei.id.startsWith("Bullet")) {
            // 子弹，带编号
            obj = com.gameengine.example.EntityFactory.createBulletVisual(renderer, ei.w, ei.h, ei.r, ei.g, ei.b, ei.a);
            

        } 
        else if (ei.id.startsWith("Decoration")) {
            // 装饰物，带编号
            obj = new GameObject(ei.id);
            obj.addComponent(new TransformComponent(new Vector2(ei.pos)));
            com.gameengine.components.RenderComponent rc = obj.addComponent(
                new com.gameengine.components.RenderComponent(
                    com.gameengine.components.RenderComponent.RenderType.CIRCLE, // 假设装饰是圆形
                    new com.gameengine.math.Vector2(Math.max(1, ei.w), Math.max(1, ei.h)),
                    new com.gameengine.components.RenderComponent.Color(ei.r, ei.g, ei.b, ei.a)
                )
            );
            rc.setRenderer(renderer);
        } 
        else {
            // 万一有未知类型，也用矩形还原
            obj = com.gameengine.example.EntityFactory.createAIVisual(renderer, Math.max(1, ei.w), Math.max(1, ei.h), ei.r, ei.g, ei.b, ei.a);
        }

        obj.setName(ei.id);

        // Transform 设置位置
        TransformComponent tc = obj.getComponent(TransformComponent.class);
        if (tc == null) obj.addComponent(new TransformComponent(new Vector2(ei.pos)));
        else tc.setPosition(new Vector2(ei.pos));

        return obj;
    }


    // ========== 文件列表模式 ==========
    private List<File> recordingFiles;
    private int selectedIndex = 0;

    private void ensureFilesListed() {
        if (recordingFiles != null) return;
        com.gameengine.recording.RecordingStorage storage = new com.gameengine.recording.FileRecordingStorage();
        recordingFiles = storage.listRecordings();
    }

    private void handleFileSelection() {
        ensureFilesListed();
        if (input.isKeyJustPressed(38) || input.isKeyJustPressed(265))
            selectedIndex = (selectedIndex - 1 + Math.max(1, recordingFiles.size())) % Math.max(1, recordingFiles.size());
        else if (input.isKeyJustPressed(40) || input.isKeyJustPressed(264))
            selectedIndex = (selectedIndex + 1) % Math.max(1, recordingFiles.size());
        else if (input.isKeyJustPressed(10) || input.isKeyJustPressed(32) || input.isKeyJustPressed(257) || input.isKeyJustPressed(335)) {
            if (recordingFiles.size() > 0) {
                String path = recordingFiles.get(selectedIndex).getAbsolutePath();
                this.recordingPath = path;
                clear();
                initialize();
            }
        } else if (input.isKeyJustPressed(27) || input.isKeyJustPressed(256) || input.isKeyJustPressed(8)) {
            engine.setScene(new MenuScene(engine, "MainMenu"));
        }
    }

    private void renderFileList() {
        ensureFilesListed();
        int w = renderer.getWidth();
        int h = renderer.getHeight();
        String title = "SELECT RECORDING";
        float tw = title.length() * 16f;
        renderer.drawText(w / 2f - tw / 2f, 80, title, 1f, 1f, 1f, 1f);

        if (recordingFiles.isEmpty()) {
            String none = "NO RECORDINGS FOUND";
            float nw = none.length() * 14f;
            renderer.drawText(w / 2f - nw / 2f, h / 2f, none, 0.9f, 0.8f, 0.2f, 1f);
            String back = "ESC TO RETURN";
            float bw = back.length() * 12f;
            renderer.drawText(w / 2f - bw / 2f, h - 60, back, 0.7f, 0.7f, 0.7f, 1f);
            return;
        }

        float startY = 140f;
        float itemH = 28f;
        for (int i = 0; i < recordingFiles.size(); i++) {
            String name = recordingFiles.get(i).getName();
            float x = 100f;
            float y = startY + i * itemH;
            if (i == selectedIndex) renderer.drawRect(x - 10, y - 6, 600, 24, 0.3f, 0.3f, 0.4f, 0.8f);
            renderer.drawText(x, y, name, 0.9f, 0.9f, 0.9f, 1f);
        }

        String hint = "UP/DOWN SELECT, ENTER PLAY, ESC RETURN";
        float hw = hint.length() * 12f;
        renderer.drawText(w / 2f - hw / 2f, h - 60, hint, 0.7f, 0.7f, 0.7f, 1f);
    }
}
