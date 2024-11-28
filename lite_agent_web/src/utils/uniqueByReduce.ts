export function uniqueByReduce(array: any[], property: string) {
    return array.reduce((acc, curr) => {
        if (!acc.some((item: { [x: string]: any; }) => item[property] === curr[property])) {
            acc.push(curr);
        }
        return acc;
    }, []);
}
