package github.kaysoro.Genquins.generator;

import github.kaysoro.Genquins.payload.Match;
import github.kaysoro.Genquins.payload.ResponseNode;
import github.kaysoro.Genquins.payload.Team;
import j2html.tags.ContainerTag;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static j2html.TagCreator.*;

public class HTMLGenerator {

    public static String generateTree(ResponseNode node) {
        //Map<id, name> des différentes équipes
        Map<String, String> teams = node.getTournament().getTeams().stream()
                .map(map -> map.values().stream().findFirst().orElse(Team.NONE))
                .collect(Collectors.toMap(Team::getId, Team::getName));

        // On ne récupère que les matches en état ouvert ou terminé de la ronde la plus récente
        List<Match> matches = node.getTournament().getMatches().stream()
                .map(map -> map.values().stream().findFirst().orElse(Match.NONE))
                .filter(match -> !match.getState().equals(Match.STATE_PENDING))
                .collect(Collectors.toList());

        int currentRound = matches.stream().map(Match::getRoundNumber).max(Integer::compare).orElse(1);
        matches = matches.stream()
                .filter(match -> match.getRoundNumber() == currentRound)
                .collect(Collectors.toList());

        ContainerTag matchesToPlay = table().withStyle("width:100%;").with(tr().with(h2("Matchs à jouer").withStyle("Margin:0;line-height:100%;font-family:arial,'helvetica neue',helvetica,sans-serif;font-size:18px;font-style:normal;font-weight:normal;color:#333333")));
        ContainerTag matchesPlayed = table().withStyle("width:100%;").with(tr().with(h2("Matchs joués").withStyle("Margin:0;line-height:100%;font-family:arial,'helvetica neue',helvetica,sans-serif;font-size:18px;font-style:normal;font-weight:normal;color:#333333")));

        for (Match match : matches) {
            if (match.getState().equals(Match.STATE_COMPLETE))
                matchesPlayed.with(tr()
                        .with(td(teams.get(match.getTeamId1())).withStyle("text-align:center;background-color:#ffced7;border-radius:8px;border:3px solid #ff0033;color:#333333;"
                                + (match.getWinnerId().equals(match.getTeamId1())?"font-weight: bold;":"text-decoration: line-through;")))
                        .with(td(match.getScores()).withStyle("font-weight: bold;text-align:center;"))
                        .with(td(teams.get(match.getTeamId2())).withStyle("text-align:center;background-color:#ffdaaa;border-radius:8px;border:3px solid orange;color:#333333;"
                                + (match.getWinnerId().equals(match.getTeamId2())?"font-weight: bold;":"text-decoration: line-through;")))
                );
            else
                matchesToPlay.with(tr()
                                .with(td(teams.get(match.getTeamId1())).withStyle("text-align:center;background-color:#c4efff;border-radius:8px;border:3px solid #147EEC;color:#333333;"))
                                .with(td(" VS ").withStyle("font-weight: bold;text-align:center;"))
                                .with(td(teams.get(match.getTeamId2())).withStyle("text-align:center;background-color:#e6c4ff;border-radius:8px;border:3px solid #8A1DB3;color:#333333;"))
                );
        }

        return p(join(matchesPlayed, br(), matchesToPlay)).renderFormatted();
    }

    public static String generateLadder(ResponseNode node) {
        //Map<id, Team> des différentes équipes
        Map<String, Team> teamsMap = node.getTournament().getTeams().stream()
                .map(map -> map.values().stream().findFirst().orElse(Team.NONE))
                .collect(Collectors.toMap(Team::getId, Function.identity()));

        // On ne récupère que les matches en état terminé de toutes les rondes joués (ou en cours)
        List<Match> matches = node.getTournament().getMatches().stream()
                .map(map -> map.values().stream().findFirst().orElse(Match.NONE))
                .filter(match -> match.getState().equals(Match.STATE_COMPLETE))
                .collect(Collectors.toList());

        for(Match match : matches){
            Team player1 = teamsMap.get(match.getTeamId1());
            Team player2 = teamsMap.get(match.getTeamId2());
            Team winner = teamsMap.get(match.getWinnerId());

            player1.setMatchNumber(player1.getMatchNumber() + 1);
            player2.setMatchNumber(player2.getMatchNumber() + 1);
            winner.setVictoryNumber(winner.getVictoryNumber() + 1);
            Matcher m = Pattern.compile("(\\d+)-(\\d+),(\\d+)-(\\d+)(,(\\d+)-(\\d+))?").matcher(match.getScores());
            if (m.matches()){
                int team1match1 = Integer.parseInt(m.group(1));
                int team2match1 = Integer.parseInt(m.group(2));
                int team1match2 = Integer.parseInt(m.group(3));
                int team2match2 = Integer.parseInt(m.group(4));
                int team1match3 = 0;
                int team2match3 = 0;
                if (m.group(5) != null){
                    team1match3 = Integer.parseInt(m.group(6));
                    team2match3 = Integer.parseInt(m.group(7));
                }
                player1.setGoalAverage(player1.getGoalAverage() + team1match1 + team1match2 + team1match3
                        - team2match1 - team2match2 - team2match3);
                player2.setGoalAverage(player2.getGoalAverage() + team2match1 + team2match2 + team2match3
                        - team1match1 - team1match2 - team1match3);
            }
        }

        // La récupération des données de match est faite, on génère le classement
        ContainerTag result = table().with(tr()
                .with(th("Equipe"))
                .with(th("Matchs"))
                .with(th("Victoires"))
                .with(th("Goal Average")))
                .attr("border", "1");
        teamsMap.values().stream()
                .sorted(Team::compareTeams)
                .collect(Collectors.toList()).forEach(team -> result.with(tr()
                .with(td(team.getName()))
                .with(td(String.valueOf(team.getMatchNumber())))
                .with(td(String.valueOf(team.getVictoryNumber()))
                .with(td(String.valueOf(team.getGoalAverage()))))
        ));

        return tr().withText("Classement").with(result).renderFormatted();
    }
}
