package models;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Xuser {
    @Id
    public String email;

    public String fullname;

    public String forename;
}
