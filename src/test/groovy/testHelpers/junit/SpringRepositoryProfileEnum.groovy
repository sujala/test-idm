package testHelpers.junit


enum SpringRepositoryProfileEnum {
    LDAP(["default", "LDAP"]),
    SQL(["SQL"])

    private List<String> profileStrings;

    private SpringRepositoryProfileEnum(profileStrings) {
        this.profileStrings = profileStrings
    }

    public static SpringRepositoryProfileEnum getProfileEnumFromProfileString(profileString) {
        for(def profileEnum : this.values()) {
            if(profileEnum.matchesProfileString(profileString)) {
                return profileEnum
            }
        }
    }

    public boolean matchesProfileString(profile) {
        profileStrings.contains(profile)
    }

}