/*
chcp 65001
javac -encoding UTF-8 Battle.java
java -Dfile.encoding=UTF-8 Battle
*/


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Battle {
    static final Scanner scanner = new Scanner(System.in);
    static final int WIDTH = 15;
    static final int HEIGHT = 11;
    static final int START_X = 1;
    static final int START_Y = 1;
    static final Random random = new Random();

    public static void main(String[] args) {
        Player player = createPlayer();
        int seed = random.nextInt(1000);
        System.out.printf("%n今回のシード値：%03d%n", seed);
        System.out.println("同じシード値を使うと、同じマップを再現できます。");
        for (int floor = 1; floor <= 3; floor++) {
            System.out.println();
            System.out.println("=================================");
            System.out.println("             地下" + floor + "階");
            System.out.println("=================================");
            char[][] map = generateMap(seed, floor);
            int playerX = START_X;
            int playerY = START_Y;
            boolean nextFloor = false;
            while (!nextFloor && player.hp > 0) {
                printMap(map, playerX, playerY, floor);
                printStatus(player);
                System.out.println();
                System.out.println("W：上　A：左　S：下　D：右");
                System.out.println("I：アイテム　T：ステータス　E：装備");
                System.out.print("入力：");
                String input = scanner.nextLine().trim().toLowerCase();
                if (input.equals("i")) {
                    useRecoveryItem(player);
                    continue;
                }
                if (input.equals("t")) {
                    printStatus(player);
                    continue;
                }
                if (input.equals("e")) {
                    printEquipment(player);
                    continue;
                }
                int nextX = playerX;
                int nextY = playerY;
                if (input.equals("w")) {
                    nextY--;
                } else if (input.equals("s")) {
                    nextY++;
                } else if (input.equals("a")) {
                    nextX--;
                } else if (input.equals("d")) {
                    nextX++;
                } else {
                    System.out.println("W・A・S・D・I・T・Eのいずれかを入力してください。");
                    continue;
                }
                if (!canMove(map, nextX, nextY)) {
                    System.out.println("そこには進めません。");
                    continue;
                }
                playerX = nextX;
                playerY = nextY;
                char tile = map[playerY][playerX];
                if (tile == 'C') {
                    openChest(player, floor);
                    map[playerY][playerX] = '.';
                } else if (tile == '>') {
                    int result = stairMenu(player, floor);
                    if (result == 1) {
                        if (floor < 3) {
                            nextFloor = true;
                        } else {
                            System.out.println();
                            System.out.println("ボスの部屋へ進みます……");
                            boolean victory = battle(player, createBoss());
                            if (victory) {
                                gameClear(player);
                                return;
                            } else {
                                break;
                            }
                        }
                    }
                }  else if (tile == 'D') {
                    int result = bossMenu(player, floor);
                    if (result == 1) {
                        System.out.println();
                        System.out.println("ボスの部屋へ進みます……");
                        boolean victory = battle(player, createBoss());
                        if (victory) {
                            gameClear(player);
                            return;
                        } else {
                            break;
                        }
                    }
                } else if (tile == '.') {
                    if (random.nextInt(100) < 25) {
                        Enemy enemy = createRandomEnemy(floor);
                        System.out.println();
                        System.out.println("敵が現れた！");
                        System.out.println(enemy.name + "が立ちはだかった！");
                        boolean victory = battle(player, enemy);
                        if (!victory) {
                            break;
                        }
                    }
                }
            }
            if (player.hp <= 0) {
                break;
            }
            if (floor < 3) {
                System.out.println();
                System.out.println("地下" + (floor + 1) + "階へ進みます……");
            }
        }
        gameOver();
    }
    // ==================================================
    // プレイヤー作成
    // ==================================================

    static Player createPlayer() {
        System.out.println("主人公の名前を入力してください。");
        System.out.print("名前：");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            name = "主人公";
        }
        System.out.println();
        System.out.println("職業を選択してください。");
        System.out.println("1：戦士");
        System.out.println("2：魔法使い");
        System.out.println("3：盗賊");
        System.out.println("4：僧侶");
        int choice = readInt("選択：", 1, 4);
        switch (choice) {
            case 2:
            return new Player(
            name, "魔法使い",
            90, 38, 8,
            "魔力増幅"
            );
            case 3:
            return new Player(
            name, "盗賊",
            105, 27, 12,
            "二回攻撃"
            );
            case 4:
            return new Player(
            name, "僧侶",
            125, 20, 15,
            "自動回復"
            );
            default:
            return new Player(
            name, "戦士",
            145, 30, 16,
            "会心の一撃"
            );
        }
    }
    // ==================================================
    // マップ生成
    // ==================================================

    static char[][] generateMap(int seed, int floor) {
        Random mapRandom =
        new Random(seed * 1000L + floor * 7919L);
        for (int attempt = 0; attempt < 100; attempt++) {
            char[][] map = new char[HEIGHT][WIDTH];
            // ① 迷路生成
            createMaze(map, mapRandom);
            // ② スタート地点
            map[START_Y][START_X] = '.';
            openArea(map, START_X, START_Y);
            // ③ 宝箱を先に配置
            placeChests(map, mapRandom, floor);
            if (floor < 3) {

            int stairsX;
            int stairsY;

                do {
                stairsX = 1 + mapRandom.nextInt(WIDTH - 2);

                // マップの下半分だけから選ぶ
                stairsY = 7 + mapRandom.nextInt(3);

                    } while (
                        map[stairsY][stairsX] != '.' ||
                        (stairsX == START_X && stairsY == START_Y) ||
                        Math.abs(stairsX - START_X) +
                        Math.abs(stairsY - START_Y) < 6
                    );

                    createGuaranteedPath(
                        map,
                        START_X,
                        START_Y,
                        stairsX,
                        stairsY
                    );

    map[stairsY][stairsX] = '>';

} else {

    // 地下3階は階段を生成しない
    int bossX = WIDTH / 2;
    int bossY = HEIGHT - 2;

    createGuaranteedPath(
            map,
            START_X,
            START_Y,
            bossX,
            bossY
    );

    map[bossY][bossX] = 'D';
}
            if (isValidMap(map, floor)) {
                return map;
            }
        }
        return createFallbackMap(floor);
    }
    // ==================================================
    // 迷路生成
    // ==================================================

    static void createMaze(char[][] map, Random mapRandom) {
        // まず内部をすべて壁にする
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                map[y][x] = '#';
            }
        }
        // 奇数座標を起点に迷路を掘る
        int startX = 1;
        int startY = 1;
        map[startY][startX] = '.';
        carveMaze(map, startX, startY, mapRandom);
        // 迷路が細かすぎないように追加の通路を作る
        for (int i = 0; i < 12; i++) {
            int x = 1 + mapRandom.nextInt(WIDTH - 2);
            int y = 1 + mapRandom.nextInt(HEIGHT - 2);
            if (map[y][x] == '#') {
                continue;
            }
            int direction = mapRandom.nextInt(4);
            int nx = x;
            int ny = y;
            if (direction == 0) {
                nx++;
            } else if (direction == 1) {
                nx--;
            } else if (direction == 2) {
                ny++;
            } else {
                ny--;
            }
            if (nx > 0 && nx < WIDTH - 1 &&
            ny > 0 && ny < HEIGHT - 1) {
                map[ny][nx] = '.';
            }
        }
    }
    static void carveMaze(
    char[][] map,
    int x,
    int y,
    Random mapRandom
    ) {
        int[] directions = {0, 1, 2, 3};
        // 毎回方向をシャッフル
        for (int i = directions.length - 1; i > 0; i--) {
            int j = mapRandom.nextInt(i + 1);
            int temp = directions[i];
            directions[i] = directions[j];
            directions[j] = temp;
        }
        for (int direction : directions) {
            int dx = 0;
            int dy = 0;
            if (direction == 0) {
                dx = 2;
            } else if (direction == 1) {
                dx = -2;
            } else if (direction == 2) {
                dy = 2;
            } else {
                dy = -2;
            }
            int nx = x + dx;
            int ny = y + dy;
            if (nx <= 0 || nx >= WIDTH - 1 ||
            ny <= 0 || ny >= HEIGHT - 1) {
                continue;
            }
            if (map[ny][nx] == '#') {
                map[y + dy / 2][x + dx / 2] = '.';
                map[ny][nx] = '.';
                carveMaze(map, nx, ny, mapRandom);
            }
        }
    }
    // ==================================================
    // 保証ルート
    // ==================================================
    static void createGuaranteedPath(
    char[][] map,
    int startX,
    int startY,
    int goalX,
    int goalY
    ) {
        int x = startX;
        int y = startY;
        map[y][x] = '.';
        // 横方向へ進む
        while (x != goalX) {
            if (x < goalX) {
                x++;
            } else {
                x--;
            }
            if (x > 0 && x < WIDTH - 1 &&
            y > 0 && y < HEIGHT - 1) {
                map[y][x] = '.';
            }
        }
        // 縦方向へ進む
        while (y != goalY) {
            if (y < goalY) {
                y++;
            } else {
                y--;
            }
            if (x > 0 && x < WIDTH - 1 &&
            y > 0 && y < HEIGHT - 1) {
                map[y][x] = '.';
            }
        }
    }

    static void openArea(char[][] map, int centerX, int centerY) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                if (x > 0 && x < WIDTH - 1 &&
                y > 0 && y < HEIGHT - 1) {
                    map[y][x] = '.';
                }
            }
        }
    }
    // ==================================================
    // 宝箱配置
    // ==================================================
    static void placeChests(
    char[][] map,
    Random mapRandom,
    int floor
    ) {
        int chestCount = 4;
        int placed = 0;
        List<Position> candidates = new ArrayList<>();
        for (int y = 1; y < HEIGHT - 1; y++) {
            for (int x = 1; x < WIDTH - 1; x++) {
                if (map[y][x] == '.') {
                    if (x == START_X && y == START_Y) {
                        continue;
                    }
                    candidates.add(new Position(x, y));
                }
            }
        }
        Collections.shuffle(candidates, mapRandom);
        for (Position position : candidates) {
            if (placed >= chestCount) {
                break;
            }
            map[position.y][position.x] = 'C';
            placed++;
        }
    }
    // ==================================================
    // マップ検証
    // ==================================================

    static boolean isValidMap(char[][] map, int floor) {
        boolean[][] visited = new boolean[HEIGHT][WIDTH];
        List<Position> queue = new ArrayList<>();
        queue.add(new Position(START_X, START_Y));
        visited[START_Y][START_X] = true;
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int i = 0; i < queue.size(); i++) {
            Position current = queue.get(i);
            for (int d = 0; d < 4; d++) {
                int nx = current.x + dx[d];
                int ny = current.y + dy[d];
                if (nx < 0 || nx >= WIDTH ||
                ny < 0 || ny >= HEIGHT) {
                    continue;
                }
                if (visited[ny][nx]) {
                    continue;
                }
                if (map[ny][nx] == '#') {
                    continue;
                }
                visited[ny][nx] = true;
                queue.add(new Position(nx, ny));
            }
        }
        if (floor < 3) {
            // 地下1・2階は階段が必要
            boolean stairsReachable = false;
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    if (map[y][x] == '>' && visited[y][x]) {
                        stairsReachable = true;
                    }
                }
            }
            if (!stairsReachable) {
                return false;
            }
        } else {
            // 地下3階はDが必要
            int bossX = WIDTH / 2;
            int bossY = HEIGHT - 2;
            if (map[bossY][bossX] != 'D') {
                return false;
            }
            if (!visited[bossY][bossX]) {
                return false;
            }
        }
        // 宝箱がすべて到達可能か確認
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (map[y][x] == 'C' && !visited[y][x]) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean canMove(char[][] map, int x, int y) {
        if (x < 0 || x >= WIDTH ||
        y < 0 || y >= HEIGHT) {
            return false;
        }
        // 壁以外はすべて移動可能
        return map[y][x] != '#';
    }
    // ==================================================
    // 安全な固定マップ
    // ==================================================

    static char[][] createFallbackMap(int floor) {
        char[][] map = new char[HEIGHT][WIDTH];
        // 全体を壁にする
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                map[y][x] = '#';
            }
        }
        // 内側を通路にする
        for (int y = 1; y < HEIGHT - 1; y++) {
            for (int x = 1; x < WIDTH - 1; x++) {
                map[y][x] = '.';
            }
        }
        // スタート地点
        map[START_Y][START_X] = '.';
        // 宝箱
        map[2][3] = 'C';
        map[4][9] = 'C';
        map[7][5] = 'C';
        map[8][10] = 'C';
        if (floor < 3) {
            // 地下1・2階だけ階段
            map[HEIGHT - 2][WIDTH / 2] = '>';
        } else {
            // 地下3階はDだけ
            map[HEIGHT - 2][WIDTH / 2] = 'D';
        }
        return map;
    }
    // ==================================================
    // マップ表示
    // ==================================================
    static void printMap(
    char[][] map,
    int playerX,
    int playerY,
    int floor
    ) {
        System.out.println();
        System.out.println("地下" + floor + "階");
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (x == playerX && y == playerY) {
                    System.out.print("P ");
                } else {
                    System.out.print(map[y][x] + " ");
                }
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("P：主人公　C：宝箱　>：階段　D：ボス");
    }
    // ==================================================
    // ステータス
    // ==================================================

    static void printStatus(Player player) {
        System.out.println();
        System.out.println("---------------------------------");
        System.out.println(
        player.name + "  Lv." + player.level +
        "  " + player.job
        );
        System.out.println(
        "HP：" + player.hp + "/" + player.maxHp
        );
        System.out.println(
        "攻撃力：" + player.getTotalAttack() +
        "  防御力：" + player.getTotalDefense()
        );
        System.out.println(
        "EXP：" + player.exp +
        "  G：" + player.gold
        );
        System.out.println(
        "ポーション：" + player.potion +
        "  メガポーション：" + player.megaPotion
        );
        System.out.println("---------------------------------");
    }

    static void printEquipment(Player player) {
        System.out.println();
        System.out.println("========== 装備 ==========");
        System.out.println(
        "武器：" + player.weaponName +
        "  攻撃力+" + player.weaponAttack
        );
        System.out.println(
        "防具：" + player.armorName +
        "  防御力+" + player.armorDefense
        );
        System.out.println("==========================");
    }
    // ==================================================
    // 階段メニュー
    // ==================================================

    static int stairMenu(Player player, int floor) {
        while (true) {
            System.out.println();
            System.out.println("=================================");
            System.out.println("              階段");
            System.out.println("=================================");
            if (floor < 3) {
                System.out.println("1：次の階へ進む");
            } else {
                System.out.println("1：ボスの部屋へ進む");
            }
            System.out.println("2：探索を続ける");
            System.out.println("3：ショップへ行く");
            int choice = readInt("選択：", 1, 3);
            if (choice == 1) {
                return 1;
            } else if (choice == 2) {
                return 0;
            } else {
                shop(player, floor);
            }
        }
    }

    static int bossMenu(Player player, int floor) {
        while (true) {
            System.out.println();
            System.out.println("=================================");
            System.out.println("           最終ボスの部屋");
            System.out.println("=================================");
            System.out.println("巨大なボスがこちらを睨んでいる……");
            System.out.println();
            System.out.println("1：バトル開始");
            System.out.println("2：探索を続ける");
            System.out.println("3：ショップへ行く");
            int choice = readInt("選択：", 1, 3);
            if (choice == 1) {
                return 1;
            } else if (choice == 2) {
                return 0;
            } else {
                shop(player, floor);
            }
        }
    }static void printShopMenu(String weaponName, String armorName,
        int weaponAttack, int armorDefense, int equipmentPrice, int gold) {
    System.out.println();
    System.out.println("=======================================================================");
    System.out.println("                                   SHOP");
    System.out.println("=======================================================================");
    System.out.println();

    System.out.println("所持金：" + gold + "G");
    System.out.println();

    System.out.println(
            padRight("番号", 8) +
            padRight("商品名", 24) +
            padRight("効果", 20) +
            padLeft("価格", 8)
    );

    System.out.println("-----------------------------------------------------------------------");

    printShopItem("1", "ポーション", "HP回復", "30G");
    printShopItem("2", "メガポーション", "HP大回復", "80G");
    printShopItem("3", weaponName, "攻撃力+" + weaponAttack, equipmentPrice + "G");
    printShopItem("4", armorName, "防御力+" + armorDefense, equipmentPrice + "G");
    printShopItem("5", "店を出る", "", "");

    System.out.println("-----------------------------------------------------------------------");
}

static void printShopItem(String number, String name, String effect, String price) {
    System.out.println(
            padRight(number, 8) +
            padRight(name, 24) +
            padRight(effect, 20) +
            padLeft(price, 8)
    );
}

static int displayWidth(String text) {
    int width = 0;

    for (char c : text.toCharArray()) {
        if (c >= 0x00 && c <= 0x7F) {
            width += 1;
        } else {
            width += 2;
        }
    }

    return width;
}

static String padRight(String text, int width) {
    StringBuilder result = new StringBuilder(text);
    int currentWidth = displayWidth(text);

    while (currentWidth < width) {
        result.append(" ");
        currentWidth++;
    }

    return result.toString();
}

static String padLeft(String text, int width) {
    int currentWidth = displayWidth(text);
    StringBuilder result = new StringBuilder();

    while (currentWidth < width) {
        result.append(" ");
        currentWidth++;
    }

    result.append(text);

    return result.toString();
}
    // ==================================================
    // ショップ
    // ==================================================

    static void shop(Player player, int floor) {
    while (true) {
        
    

        String weaponName;
        String armorName;
        int weaponAttack;
        int armorDefense;
        int equipmentPrice;

        // 階層ごとの性能・価格
        if (floor == 1) {
            weaponAttack = 10;
            armorDefense = 5;
            equipmentPrice = 100;
        } else if (floor == 2) {
            weaponAttack = 20;
            armorDefense = 10;
            equipmentPrice = 180;
        } else {
            weaponAttack = 30;
            armorDefense = 15;
            equipmentPrice = 300;
        }

        // 職業ごとの装備名
        if (player.job.equals("戦士")) {
            if (floor == 1) {
                weaponName = "鉄の剣";
                armorName = "鉄の鎧";
            } else if (floor == 2) {
                weaponName = "鋼の剣";
                armorName = "鋼の鎧";
            } else {
                weaponName = "聖なる剣";
                armorName = "聖なる鎧";
            }
        } else if (player.job.equals("魔法使い")) {
            if (floor == 1) {
                weaponName = "魔導杖";
                armorName = "魔法のローブ";
            } else if (floor == 2) {
                weaponName = "賢者の杖";
                armorName = "賢者のローブ";
            } else {
                weaponName = "聖魔の杖";
                armorName = "聖者のローブ";
            }
        } else if (player.job.equals("盗賊")) {
            if (floor == 1) {
                weaponName = "鉄の短剣";
                armorName = "革の服";
            } else if (floor == 2) {
                weaponName = "疾風の短剣";
                armorName = "盗賊の服";
            } else {
                weaponName = "影の短剣";
                armorName = "影のマント";
            }
        } else if (player.job.equals("僧侶")) {
            if (floor == 1) {
                weaponName = "聖なる杖";
                armorName = "聖職者の服";
            } else if (floor == 2) {
                weaponName = "祝福の杖";
                armorName = "神官の法衣";
            } else {
                weaponName = "神聖な杖";
                armorName = "聖者の法衣";
            }
        } else {
            weaponName = "鉄の剣";
            armorName = "鉄の鎧";
        }

        printShopMenu(
        weaponName,
        armorName,
        weaponAttack,
        armorDefense,
        equipmentPrice,
        player.gold
);

        int choice = readInt("選択：", 1, 5);

        if (choice == 1) {
            if (player.gold >= 30) {
                player.gold -= 30;
                player.potion++;
                System.out.println("ポーションを購入しました。");
            } else {
                System.out.println("ゴールドが足りません。");
            }
        } else if (choice == 2) {
            if (player.gold >= 80) {
                player.gold -= 80;
                player.megaPotion++;
                System.out.println("メガポーションを購入しました。");
            } else {
                System.out.println("ゴールドが足りません。");
            }
        } else if (choice == 3) {
            buyWeapon(player, weaponName, weaponAttack, equipmentPrice);
        } else if (choice == 4) {
            buyArmor(player, armorName, armorDefense, equipmentPrice);
        } else {
            return;
        }
    }
}
    static void buyWeapon(
    Player player,
    String name,
    int attack,
    int price
    ) {
        if (attack <= player.weaponAttack) {
            System.out.println("現在の武器より弱いため購入できません。");
            return;
        }
        if (player.gold < price) {
            System.out.println("ゴールドが足りません。");
            return;
        }
        player.gold -= price;
        player.weaponName = name;
        player.weaponAttack = attack;
        System.out.println(name + "を購入しました！");
    }
    static void buyArmor(
    Player player,
    String name,
    int defense,
    int price
    ) {
        if (defense <= player.armorDefense) {
            System.out.println("現在の防具より弱いため購入できません。");
            return;
        }
        if (player.gold < price) {
            System.out.println("ゴールドが足りません。");
            return;
        }
        player.gold -= price;
        player.armorName = name;
        player.armorDefense = defense;
        System.out.println(name + "を購入しました！");
    }
    // ==================================================
    // 宝箱
    // ==================================================

    static void openChest(Player player, int floor) {
        System.out.println();
        System.out.println("宝箱を発見した！");
        int result = random.nextInt(100);
        if (result < 35) {
            int gold = 50 + random.nextInt(101);
            player.gold += gold;
            System.out.println(gold + "Gを手に入れた！");
        } else if (result < 60) {
            player.potion++;
            System.out.println("ポーションを手に入れた！");
        } else if (result < 75) {
            player.megaPotion++;
            System.out.println("メガポーションを手に入れた！");
        } else if (result < 90) {
            String weaponName;
            // 職業ごとの武器名
            if (player.job.equals("戦士")) {
                if (floor == 1) {
                    weaponName = "銅の剣";
                } else if (floor == 2) {
                    weaponName = "銀の剣";
                } else {
                    weaponName = "魔法の剣";
                }
            } else if (player.job.equals("魔法使い")) {
                if (floor == 1) {
                    weaponName = "銅の杖";
                } else if (floor == 2) {
                    weaponName = "銀の杖";
                } else {
                    weaponName = "魔法の杖";
                }
            } else if (player.job.equals("盗賊")) {
                if (floor == 1) {
                    weaponName = "銅の短剣";
                } else if (floor == 2) {
                    weaponName = "銀の短剣";
                } else {
                    weaponName = "魔法の短剣";
                }
            } else {
                if (floor == 1) {
                    weaponName = "銅の杖";
                } else if (floor == 2) {
                    weaponName = "銀の杖";
                } else {
                    weaponName = "聖なる杖";
                }
            }
            // 階層ごとの攻撃力
            int weaponAttack;
            if (floor == 1) {
                weaponAttack = 3;
            } else if (floor == 2) {
                weaponAttack = 7;
            } else {
                weaponAttack = 11;
            }
            receiveWeapon(player, weaponName, weaponAttack);
        } else {
            String armorName;
            // 職業ごとの防具名
            if (player.job.equals("戦士")) {
                if (floor == 1) {
                    armorName = "銅の鎧";
                } else if (floor == 2) {
                    armorName = "銀の鎧";
                } else {
                    armorName = "魔法の鎧";
                }
            } else if (player.job.equals("魔法使い")) {
                if (floor == 1) {
                    armorName = "銅のローブ";
                } else if (floor == 2) {
                    armorName = "銀のローブ";
                } else {
                    armorName = "魔法のローブ";
                }
            } else if (player.job.equals("盗賊")) {
                if (floor == 1) {
                    armorName = "銅の服";
                } else if (floor == 2) {
                    armorName = "銀の服";
                } else {
                    armorName = "魔法のマント";
                }
            } else {
                if (floor == 1) {
                    armorName = "銅の法衣";
                } else if (floor == 2) {
                    armorName = "銀の法衣";
                } else {
                    armorName = "聖なる法衣";
                }
            }
            // 階層ごとの防御力
            int armorDefense;
            if (floor == 1) {
                armorDefense = 3;
            } else if (floor == 2) {
                armorDefense = 7;
            } else {
                armorDefense = 11;
            }
            receiveArmor(player, armorName, armorDefense);
        }
    }
    static void receiveWeapon(
    Player player,
    String name,
    int attack
    ) {
        System.out.println(name + "を手に入れた！");
        if (attack > player.weaponAttack) {
            player.weaponName = name;
            player.weaponAttack = attack;
            System.out.println(name + "を装備しました！");
        } else {
            System.out.println("現在の武器より弱いため売却され、50Gになった。");
            player.gold += 50;
        }
    }
    static void receiveArmor(
    Player player,
    String name,
    int defense
    ) {
        System.out.println(name + "を手に入れた！");
        if (defense > player.armorDefense) {
            player.armorName = name;
            player.armorDefense = defense;
            System.out.println(name + "を装備しました！");
        } else {
            System.out.println("現在の防具より弱いため売却され、50Gになった。");
            player.gold += 50;
        }
    }
    // ==================================================
    // 敵作成
    // ==================================================

    static Enemy createRandomEnemy(int floor) {
        int result = random.nextInt(4);
        if (floor == 1) {
            if (result == 0) {
                return new Enemy("スライム", 45, 12, 4, 20, 15);
            } else if (result == 1) {
                return new Enemy("ゴブリン", 60, 17, 7, 30, 20);
            } else if (result == 2) {
                return new Enemy("ウルフ", 70, 20, 8, 35, 25);
            } else {
                return new Enemy("オーク", 90, 25, 10, 50, 35);
            }
        } else if (floor == 2) {
            if (result == 0) {
                return new Enemy("強化スライム", 90, 22, 10, 50, 40);
            } else if (result == 1) {
                return new Enemy("リザードマン", 110, 27, 13, 65, 50);
            } else if (result == 2) {
                return new Enemy("キメラ", 130, 32, 15, 80, 60);
            } else {
                return new Enemy("トロール", 160, 37, 18, 100, 80);
            }
        } else {
            if (result == 0) {
                return new Enemy("ダークスライム", 120, 28, 15, 80, 65);
            } else if (result == 1) {
                return new Enemy("デーモン", 150, 33, 19, 110, 85);
            } else if (result == 2) {
                return new Enemy("ドラゴンナイト", 180, 38, 22, 140, 110);
            } else {
                return new Enemy("ヘルオーク", 210, 43, 25, 170, 130);
            }
        }
    }

    static Enemy createBoss() {
        return new Enemy(
        "迷宮の魔王",
        500,
        55,
        30,
        1000,
        1000
        );
    }
    // ==================================================
    // 戦闘
    // ==================================================

    static boolean battle(Player player, Enemy enemy) {
        while (player.hp > 0 && enemy.hp > 0) {
            System.out.println();
            System.out.println("=================================");
            System.out.println("             BATTLE");
            System.out.println("=================================");
            System.out.println(
            enemy.name + " HP：" +
            enemy.hp + "/" + enemy.maxHp
            );
            System.out.println(
            player.name + " HP：" +
            player.hp + "/" + player.maxHp
            );
            System.out.println();
            System.out.println("1：攻撃");
            System.out.println("2：アイテム");
            System.out.println("3：逃げる");
            int choice = readInt("選択：", 1, 3);
            boolean actionCompleted = false;
            if (choice == 1) {
                playerAttack(player, enemy);
                actionCompleted = true;
            } else if (choice == 2) {
                actionCompleted = useRecoveryItem(player);
            } else {
                if (enemy.name.equals("迷宮の魔王")) {
                    System.out.println("ボス戦からは逃げられません！");
                } else if (random.nextInt(100) < 50) {
                    System.out.println("逃げ切った！");
                    return true;
                } else {
                    System.out.println("逃げられなかった！");
                    actionCompleted = true;
                }
            }
            if (!actionCompleted) {
                continue;
            }
            if (enemy.hp <= 0) {
                break;
            }
            enemyAttack(player, enemy);
            if (player.hp <= 0) {
                break;
            }
            if (player.job.equals("僧侶")) {
                int heal = 5;
                player.hp += heal;
                if (player.hp > player.maxHp) {
                    player.hp = player.maxHp;
                }
                System.out.println("僧侶の自動回復！");
                System.out.println("HPが" + heal + "回復した。");
            }
        }
        if (player.hp <= 0) {
            return false;
        }
        System.out.println();
        System.out.println(enemy.name + "を倒した！");
        player.gold += enemy.gold;
        player.exp += enemy.exp;
        System.out.println(enemy.gold + "Gを手に入れた！");
        System.out.println(enemy.exp + "EXPを獲得した！");
        levelUp(player);
        return true;
    }

    static void playerAttack(Player player, Enemy enemy) {
        int damage = player.getTotalAttack();
        if (player.job.equals("魔法使い")) {
            damage += 10;
            System.out.println("魔力増幅が発動した！");
        } else if (player.job.equals("盗賊")) {
            damage += player.getTotalAttack();
            System.out.println("二回攻撃が発動した！");
        } else if (player.job.equals("戦士")) {
            if (random.nextInt(100) < 25) {
                damage *= 2;
                System.out.println("会心の一撃！");
            }
        }
        int finalDamage = damage - enemy.defense;
        if (finalDamage < 1) {
            finalDamage = 1;
        }
        enemy.hp -= finalDamage;
        System.out.println(player.name + "の攻撃！");
        System.out.println(enemy.name + "に" + finalDamage + "ダメージ！");
    }

    static void enemyAttack(Player player, Enemy enemy) {
        int damage = enemy.attack - player.getTotalDefense();
        if (damage < 1) {
            damage = 1;
        }
        player.hp -= damage;
        System.out.println(enemy.name + "の攻撃！");
        System.out.println(player.name + "は" + damage + "ダメージを受けた！");
    }
    // ==================================================
    // 回復アイテム
    // ==================================================

    static boolean useRecoveryItem(Player player) {
        System.out.println();
        System.out.println("1：ポーション");
        System.out.println("2：メガポーション");
        System.out.println("3：戻る");
        int choice = readInt("選択：", 1, 3);
        if (choice == 1) {
            if (player.potion <= 0) {
                System.out.println("ポーションを持っていません。");
                return false;
            }
            if (player.hp == player.maxHp) {
                System.out.println("HPは満タンです。");
                return false;
            }
            player.potion--;
            int heal = 50;
            player.hp += heal;
            if (player.hp > player.maxHp) {
                player.hp = player.maxHp;
            }
            System.out.println("ポーションを使用した！");
            System.out.println("HPが" + heal + "回復した！");
            return true;
        } else if (choice == 2) {
            if (player.megaPotion <= 0) {
                System.out.println("メガポーションを持っていません。");
                return false;
            }
            if (player.hp == player.maxHp) {
                System.out.println("HPは満タンです。");
                return false;
            }
            player.megaPotion--;
            int heal = 120;
            player.hp += heal;
            if (player.hp > player.maxHp) {
                player.hp = player.maxHp;
            }
            System.out.println("メガポーションを使用した！");
            System.out.println("HPが" + heal + "回復した！");
            return true;
        } else {
            return false;
        }
    }
    // ==================================================
    // レベルアップ
    // ==================================================

    static void levelUp(Player player) {
        while (player.exp >= player.getRequiredExp()) {
            int oldMaxHp = player.maxHp;
            player.level++;
            player.maxHp += 10;
            player.attack += 3;
            player.defense += 2;
            int hpIncrease = player.maxHp - oldMaxHp;
            player.hp += hpIncrease;
            if (player.hp > player.maxHp) {
                player.hp = player.maxHp;
            }
            System.out.println();
            System.out.println("=================================");
            System.out.println("          LEVEL UP！");
            System.out.println("=================================");
            System.out.println("レベル" + player.level + "になった！");
            System.out.println("最大HPが" + hpIncrease + "増加した！");
            System.out.println("攻撃力が3増加した！");
            System.out.println("防御力が2増加した！");
            System.out.println("HPが" + hpIncrease + "回復した！");
        }
    }
    // ==================================================
    // 結果表示
    // ==================================================

    static void gameClear(Player player) {
        System.out.println();
        System.out.println("=================================");
        System.out.println("          GAME CLEAR！");
        System.out.println("=================================");
        System.out.println("迷宮の魔王を討伐した！");
        System.out.println("最終レベル：" + player.level);
        System.out.println("最終ゴールド：" + player.gold + "G");
    }

    static void gameOver() {
        System.out.println();
        System.out.println("=================================");
        System.out.println("          GAME OVER");
        System.out.println("=================================");
    }
    // ==================================================
    // 入力処理
    // ==================================================

    static int readInt(String message, int min, int max) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // 数字以外は下のメッセージを表示
            }
            System.out.println(
            min + "～" + max + "の数字を入力してください。"
            );
        }
    }
    // ==================================================
    // プレイヤークラス
    // ==================================================
    static class Player {
        String name;
        String job;
        String passive;
        int level;
        int exp;
        int gold;
        int hp;
        int maxHp;
        int attack;
        int defense;
        String weaponName;
        int weaponAttack;
        String armorName;
        int armorDefense;
        int potion;
        int megaPotion;
        Player(
        String name,
        String job,
        int maxHp,
        int attack,
        int defense,
        String passive
        ) {
            this.name = name;
            this.job = job;
            this.passive = passive;
            this.level = 1;
            this.exp = 0;
            this.gold = 100;
            this.maxHp = maxHp;
            this.hp = maxHp;
            this.attack = attack;
            this.defense = defense;
            this.weaponName = "なし";
            this.weaponAttack = 0;
            this.armorName = "布の服";
            this.armorDefense = 0;
            this.potion = 2;
            this.megaPotion = 0;
        }
        int getTotalAttack() {
            return attack + weaponAttack;
        }
        int getTotalDefense() {
            return defense + armorDefense;
        }
        int getRequiredExp() {
            return 30 * level * level;
        }
    }
    // ==================================================
    // 敵クラス
    // ==================================================
    static class Enemy {
        String name;
        int hp;
        int maxHp;
        int attack;
        int defense;
        int exp;
        int gold;
        Enemy(
        String name,
        int hp,
        int attack,
        int defense,
        int exp,
        int gold
        ) {
            this.name = name;
            this.hp = hp;
            this.maxHp = hp;
            this.attack = attack;
            this.defense = defense;
            this.exp = exp;
            this.gold = gold;
        }
    }
    // ==================================================
    // 座標クラス
    // ==================================================
    static class Position {
        int x;
        int y;
        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
