package pro.hirooka.chukasa.domain;

import lombok.Data;

@Data
public class EPGResponseModel {
    int ch;
    int genre;
    long begin;
    long id;
    String title;
    String summary;
}
