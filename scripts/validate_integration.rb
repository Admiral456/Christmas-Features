#!/usr/bin/env ruby
# Lightweight release gate for the ChristmasFeatures integration configs.
# Uses Ruby's standard YAML/JSON libraries so CI needs no extra dependencies.
require "yaml"
require "json"
require "pathname"

def fail!(message)
  warn "INTEGRATION VALIDATION FAILED: #{message}"
  exit 1
end

def load_yaml(path)
  fail!("missing file: #{path}") unless File.file?(path)
  YAML.safe_load_file(path, permitted_classes: [], aliases: false) || {}
rescue Psych::Exception => e
  fail!("invalid YAML in #{path}: #{e.message}")
end

plugin = load_yaml("src/main/resources/plugin.yml")
config = load_yaml("src/main/resources/config.yml")
grinch_doc = load_yaml("integrations/MythicMobs/Mobs/Christmas/ChristmasGrinch.yml")
yeti_doc = load_yaml("integrations/MythicMobs/Mobs/Christmas/ChristmasYeti.yml")
frost_doc = load_yaml("integrations/MythicMobs/Mobs/Christmas/ChristmasFrostEmperor.yml")
spawns = load_yaml("integrations/MythicMobs/RandomSpawns/ChristmasMountains.yml")

pom = File.read("pom.xml")
pom_version = pom[/<artifactId>christmas-features<\/artifactId>\s*<version>([^<]+)<\/version>/, 1]
fail!("pom.xml version does not match plugin.yml") unless pom_version == plugin["version"]
fail!("expected a bumped 1.0.9 release") unless plugin["version"] == "1.0.9"

preserved = Array(config.dig("winter", "preserve-biomes"))
%w[SNOWY_SLOPES FROZEN_PEAKS JAGGED_PEAKS GROVE].each do |biome|
  fail!("winter.preserve-biomes is missing #{biome}") unless preserved.include?(biome)
end

grinch = grinch_doc["ChristmasGrinch"] || fail!("ChristmasGrinch definition missing")
grinch_options = grinch["Options"] || {}
fail!("Grinch must not use NoAI: true (it prevents MythicMobs skills)") if grinch_options["NoAI"] == true
fail!("Grinch must use persistent despawn mode") unless grinch_options["Despawn"].to_s.upcase == "PERSISTENT"
fail!("Grinch must clear vanilla AI goals but keep skills active") unless Array(grinch["AIGoalSelectors"]).include?("clear")
grinch_skills = Array(grinch["Skills"]).join("\n")
%w[meg:model{mid=grinch meg:headtarget{p=head addtag{t=christmas_grinch].each do |required|
  fail!("Grinch missing required skill fragment: #{required}") unless grinch_skills.include?(required)
end
fail!("Grinch model must save on entity unload, use state-machine animations and use tag_name anchor id") unless grinch_skills.include?("save=true") && grinch_skills.include?("usm=true") && grinch_skills.include?("n=name")

{
  "ChristmasYeti" => [yeti_doc, "yeti"],
  "ChristmasFrostEmperor" => [frost_doc, "frost_emperor"]
}.each do |mob_id, (document, model_id)|
  mob = document[mob_id] || fail!("#{mob_id} definition missing")
  skills = Array(mob["Skills"]).join("\n")
  fail!("#{mob_id} does not apply ModelEngine model #{model_id}") unless skills.include?("meg:model{mid=#{model_id}")
  fail!("#{mob_id} model is not saved on chunk unload") unless skills.include?("save=true")
  fail!("#{mob_id} does not use ModelEngine state-machine animations") unless skills.include?("usm=true")
  fail!("#{mob_id} has incorrect nametag anchor") unless skills.include?("n=name")
  fail!("#{mob_id} does not initialize idle animation") unless skills.include?("s=idle")
  fail!("#{mob_id} does not track nearby players") unless skills.include?("meg:headtarget{p=head")
end
yeti_skills = Array(yeti_doc.dig("ChristmasYeti", "Skills")).join("\n")
fail!("Yeti roar must use onAttack, not a ThreatTable-dependent trigger") unless yeti_skills.include?("s=roar") && yeti_skills.include?("~onAttack")

{
  "ChristmasYetiMountains" => ["ChristmasYeti", 0.01, %w[SNOWY_SLOPES FROZEN_PEAKS GROVE], 180],
  "ChristmasFrostEmperorMountains" => ["ChristmasFrostEmperor", 0.002, %w[FROZEN_PEAKS JAGGED_PEAKS], 900]
}.each do |spawn_id, (mob, chance, biomes, cooldown)|
  rule = spawns[spawn_id] || fail!("missing random-spawn rule #{spawn_id}")
  fail!("#{spawn_id} has wrong Type") unless rule["Type"] == mob
  fail!("#{spawn_id} must be limited to world") unless rule["Worlds"] == "world"
  fail!("#{spawn_id} must use Action: ADD") unless rule["Action"] == "ADD"
  fail!("#{spawn_id} has incorrect Chance") unless rule["Chance"].to_f == chance
  fail!("#{spawn_id} has incorrect Cooldown") unless rule["Cooldown"].to_i == cooldown
  actual_biomes = rule["Biomes"].to_s.split(",").map(&:strip)
  fail!("#{spawn_id} biome filter mismatch") unless actual_biomes.sort == biomes.sort
  fail!("#{spawn_id} must use PositionType: LAND") unless rule["PositionType"] == "LAND"
end

fail!("Grinch tags menu is not player-context /tags") unless config.dig("grinch", "menu", "tags-command") == "player:tags"
fail!("Grinch quests menu is not player-context BattlePass quests") unless config.dig("grinch", "menu", "quests-command") == "player:bp open quests"
fail!("deployment diagnostics helper is missing") unless File.file?("src/main/java/cz/vanocni/features/ChristmasDiagnostics.java")
command_source = File.read("src/main/java/cz/vanocni/features/ChristmasCommand.java")
fail!("diagnostics command is not registered") unless command_source.include?('case "diagnostics" -> new ChristmasDiagnostics(plugin).print(sender);')

Dir.glob("resourcepack/**/*.json").each do |path|
  begin
    JSON.parse(File.read(path))
  rescue JSON::ParserError => e
    fail!("invalid resource-pack JSON in #{path}: #{e.message}")
  end
end
fail!("resource-pack metadata is missing") unless File.file?("resourcepack/pack.mcmeta")

puts "Integration validation passed: versions, YAML, mountain biomes, MythicMobs, Grinch menus, and resource-pack JSON."
